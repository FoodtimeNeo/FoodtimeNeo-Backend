package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.config.properties.DailyRecommendationProperties;
import com.FoodtimeNeo.recommendation.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DailyRecommendationIT extends ReadApiITSupport {
    @Autowired DailyRecommendationService service;
    @Autowired DailyRecommendationStore store;
    @Autowired DailyRecommendationGenerator generator;
    @Autowired DailyRecommendationCacheService cache;
    @Autowired DailyRecommendationProperties config;
    @Autowired PlatformTransactionManager transactions;
    @Autowired Clock clock;
    @Autowired StringRedisTemplate redis;
    final Set<LocalDate> dates = new HashSet<>();
    List<UUID> previouslyActive;

    @BeforeEach void isolateCatalogueInDisposableTestDatabase() {
        previouslyActive = jdbc.queryForList("update foodtime.dishes set status='inactive' where status='active' returning id", UUID.class);
    }

    LocalDate testDate(int day) {
        LocalDate date = LocalDate.of(2041, 1, 1).plusDays(day);
        dates.add(date);
        return date;
    }

    List<UUID> catalogue(int count) {
        List<UUID> result = new ArrayList<>();
        for (int h = 0; h < 4; h++) {
            UUID hall = hall("active");
            for (int s = 0; s < 3; s++) {
                UUID stall = stall(hall, "active");
                for (int d = h * 3 + s; d < count; d += 12) result.add(dish(stall, "active", "12.50"));
            }
        }
        return result;
    }

    DailyRecommendationService instance(DailyRecommendationStore source, DailyRecommendationCacheService caching) {
        return new DailyRecommendationService(source, caching, config, generator, clock, transactions);
    }

    @Override protected void cleanContentFixtures() {
        for (var date : dates) {
            jdbc.update("delete from foodtime.daily_recommendation_batch where recommendation_date = ?", date);
            redis.delete(cache.key(date));
        }
        for (var dish : dishIds) {
            jdbc.update("delete from foodtime.dish_images where dish_id = ?", dish);
            jdbc.update("delete from foodtime.dish_review_submissions where dish_id = ?", dish);
            jdbc.update("delete from foodtime.dish_reviews where dish_id = ?", dish);
        }
        jdbc.batchUpdate("update foodtime.dishes set status='active' where id=?",
                previouslyActive.stream().map(id -> new Object[]{id}).toList());
    }

    @Test void freshSchemaGeneratesAndRepeatedExecutionReturnsIdenticalSnapshot() {
        catalogue(40);
        LocalDate date = testDate(0);
        var first = service.get(date);
        var batch = store.findSuccessfulBatch(date);
        var second = service.get(date);
        assertThat(first).isEqualTo(second);
        assertThat(first.items()).hasSize(10);
        assertThat(service.generate(date).id()).isEqualTo(batch.id());
        assertThat(jdbc.queryForObject("select count(*) from foodtime.daily_recommendation_batch where recommendation_date = ?", Long.class, date)).isEqualTo(1L);
        assertThat(store.findExposure(date.minusDays(90), date.plusDays(1)).values()).allMatch(e -> e.count() == 1);
    }

    @Test void concurrentIndependentInstancesCommitOnlyOneCompleteBatch() throws Exception {
        catalogue(60);
        LocalDate date = testDate(1);
        var cacheStub = mock(DailyRecommendationCacheService.class);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(6)) {
            List<Future<UUID>> jobs = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                var independent = instance(new DailyRecommendationStore(jdbc), cacheStub);
                jobs.add(executor.submit(() -> {
                    start.await();
                    return independent.generate(date).id();
                }));
            }
            start.countDown();
            Set<UUID> ids = new HashSet<>();
            for (var job : jobs) ids.add(job.get(30, TimeUnit.SECONDS));
            assertThat(ids).hasSize(1);
            assertThat(store.findVisibleItems(ids.iterator().next())).hasSize(10);
            assertThat(jdbc.queryForObject("select count(*) from foodtime.daily_recommendation_batch where recommendation_date = ? and generation_status = 'success'", Long.class, date)).isEqualTo(1L);
        }
    }

    @Test void partialWriteRollsBackFailureIsRecordedAndLazyRetrySucceeds() {
        catalogue(30);
        LocalDate date = testDate(2);
        var failingStore = spy(new DailyRecommendationStore(jdbc));
        var caching = mock(DailyRecommendationCacheService.class);
        doCallRealMethod().doThrow(new IllegalStateException("simulated write failure"))
                .when(failingStore).insertItem(any(), any());
        var failing = instance(failingStore, caching);
        assertThatIllegalStateException().isThrownBy(() -> failing.generate(date));
        assertThat(store.findSuccessfulBatch(date)).isNull();
        assertThat(jdbc.queryForObject("select generation_status from foodtime.daily_recommendation_batch where recommendation_date = ?", String.class, date)).isEqualTo("failed");
        assertThat(jdbc.queryForObject("select count(*) from foodtime.daily_recommendation_item i join foodtime.daily_recommendation_batch b on b.id=i.batch_id where b.recommendation_date = ?", Long.class, date)).isZero();
        verify(caching, never()).put(any(), any(), any());
        assertThat(service.get(date).items()).hasSize(10);
        var success = store.findSuccessfulBatch(date);
        store.recordFailure(date, "other-version", "other-config");
        assertThat(store.findSuccessfulBatch(date)).isEqualTo(success);
    }

    @Test void cacheIsWrittenOnlyAfterDatabaseCommit() {
        catalogue(20);
        LocalDate date = testDate(3);
        var caching = mock(DailyRecommendationCacheService.class);
        doAnswer(call -> {
            // A separate connection can see success and every item only after commit.
            try (var connection = jdbc.getDataSource().getConnection();
                 var statement = connection.prepareStatement("select count(*) from foodtime.daily_recommendation_item i join foodtime.daily_recommendation_batch b on b.id=i.batch_id where b.recommendation_date=? and b.generation_status='success'")) {
                statement.setObject(1, date);
                try (var result = statement.executeQuery()) {
                    result.next();
                    assertThat(result.getInt(1)).isEqualTo(10);
                }
            }
            return null;
        }).when(caching).put(eq(date), any(), anyList());
        instance(store, caching).generate(date);
        verify(caching).put(eq(date), any(), anyList());
    }

    @Test void redisReadAndWriteFailureFallsBackToCommittedDatabase() {
        catalogue(20);
        LocalDate date = testDate(4);
        var template = mock(StringRedisTemplate.class);
        when(template.opsForValue()).thenThrow(new IllegalStateException("Redis offline"));
        var unavailableCache = new DailyRecommendationCacheService(template, config);
        var degraded = instance(store, unavailableCache);
        var first = degraded.get(date);
        assertThat(degraded.get(date)).isEqualTo(first);
        assertThat(first.items()).hasSize(10);
        assertThat(store.findSuccessfulBatch(date)).isNotNull();
    }

    @Test void dishStallHallAndImageVisibilityAreRecheckedWithoutRewritingHistory() {
        var dishes = catalogue(10);
        LocalDate date = testDate(5);
        var response = service.get(date);
        var first = response.items().getFirst();
        UUID uploader = user("user");
        jdbc.update("insert into foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status) values (?,?,?, '/unapproved.jpg','pending')", UUID.randomUUID(), first.dishId(), uploader);
        jdbc.update("insert into foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at) values (?,?,?, '/approved.jpg','approved',?,now())", UUID.randomUUID(), first.dishId(), uploader, uploader);
        assertThat(service.get(date).items().stream().filter(i -> i.dishId().equals(first.dishId())).findFirst().orElseThrow().imageUrl()).isEqualTo("/approved.jpg");
        jdbc.update("update foodtime.dish_images set status='rejected', reviewed_by_user_id=?, reviewed_at=now() where dish_id=?", uploader, first.dishId());
        assertThat(service.get(date).items().stream().filter(i -> i.dishId().equals(first.dishId())).findFirst().orElseThrow().imageUrl()).isNull();
        jdbc.update("update foodtime.dishes set status='inactive' where id=?", first.dishId());
        assertThat(service.get(date).items()).noneMatch(i -> i.dishId().equals(first.dishId()));
        jdbc.update("update foodtime.stalls set status='inactive' where id=?", response.items().get(1).stallId());
        assertThat(service.get(date).items()).noneMatch(i -> i.stallId().equals(response.items().get(1).stallId()));
        jdbc.update("update foodtime.dining_halls set status='inactive' where id=?", response.items().get(2).diningHallId());
        assertThat(service.get(date).items()).noneMatch(i -> i.diningHallId().equals(response.items().get(2).diningHallId()));
        assertThat(jdbc.queryForObject("select count(*) from foodtime.daily_recommendation_item where batch_id=?", Long.class, store.findSuccessfulBatch(date).id())).isEqualTo(10L);
    }

    @Test void candidateQueryExcludesInactiveParentsAndPendingReviews() {
        UUID hall = hall("active"), stall = stall(hall, "active");
        UUID valid = dish(stall, "active", "10");
        dish(stall, "inactive", "10");
        dish(stall(hall("inactive"), "active"), "active", "10");
        dish(stall(hall, "inactive"), "active", "10");
        UUID user = user("user");
        jdbc.update("insert into foodtime.dish_review_submissions(id,dish_id,user_id,rating,content) values (?,?,?,5,'pending')", UUID.randomUUID(), valid, user);
        var result = store.findCandidates(Instant.parse("2000-01-01T00:00:00Z"), Instant.parse("2100-01-01T00:00:00Z"));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().reviewCount()).isZero();
        assertThat(result.getFirst().reviews7d()).isZero();
        assertThat(store.findGlobalAverage()).isEqualTo(3.5);
    }

    @Test void reviewEditsAndSoftDeletesReuseExistingStatsAndNeverInflateHeat() {
        UUID dish = catalogue(1).getFirst(), user = user("user"), review = UUID.randomUUID();
        jdbc.update("insert into foodtime.dish_reviews(id,dish_id,user_id,rating,content,created_at) values (?,?,?,5,'review',?)",
                review, dish, user, Timestamp.from(Instant.parse("2040-12-30T00:00:00Z")));
        var from = Instant.parse("2040-12-25T00:00:00Z");
        var to = Instant.parse("2041-01-01T00:00:00Z");
        assertThat(store.findCandidates(from, to).getFirst().reviews7d()).isEqualTo(1);
        assertThat(store.findGlobalAverage()).isEqualTo(5);
        jdbc.update("update foodtime.dish_reviews set rating=1, content='edited' where id=?", review);
        var edited = store.findCandidates(from, to).getFirst();
        assertThat(edited.reviewCount()).isEqualTo(1);
        assertThat(edited.reviews7d()).isEqualTo(1);
        assertThat(edited.averageRating()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(store.findGlobalAverage()).isEqualTo(1);
        jdbc.update("update foodtime.dish_reviews set deleted_at=now() where id=?", review);
        var deleted = store.findCandidates(from, to).getFirst();
        assertThat(deleted.reviewCount()).isZero();
        assertThat(deleted.reviews7d()).isZero();
        assertThat(deleted.averageRating()).isNull();
        assertThat(store.findGlobalAverage()).isEqualTo(3.5);
    }

    @Test void historyCountWindowAndLastDateAreIndependentAndIgnoreFailedBatches() {
        var dishes = catalogue(1);
        LocalDate date = testDate(200);
        LocalDate old = date.minusDays(100);
        dates.add(old);
        var batch = service.generate(old);
        var exposure = store.findExposure(date.minusDays(90), date).get(dishes.getFirst());
        assertThat(exposure.count()).isZero();
        assertThat(exposure.lastDate()).isEqualTo(old);
        LocalDate failedDate = testDate(199);
        store.recordFailure(failedDate, config.algorithmVersion(), config.configHash());
        var failedId = jdbc.queryForObject("select id from foodtime.daily_recommendation_batch where recommendation_date=?", UUID.class, failedDate);
        store.insertItem(failedId, new RecommendationModels.StoredItem(dishes.getFirst(), 1, "EXPLORE", .5, "LESS_EXPOSED"));
        assertThat(store.findVisibleItems(failedId)).isEmpty();
        assertThat(store.findExposure(date.minusDays(90), date).get(dishes.getFirst())).isEqualTo(exposure);
        assertThat(batch.generatedAt()).isNotNull();
    }

    @Test void databaseEnforcesUniqueDateDishPositionAndPositivePosition() {
        var dishes = catalogue(12);
        LocalDate date = testDate(6);
        var batch = service.generate(date);
        var first = store.findVisibleItems(batch.id()).getFirst();
        assertThatThrownBy(() -> store.insertItem(batch.id(), new RecommendationModels.StoredItem(
                first.dishId(), 11, "EXPLORE", .5, "DAILY_DISCOVERY"))).isInstanceOf(DataIntegrityViolationException.class);
        var spare = dishes.stream().filter(d -> store.findVisibleItems(batch.id()).stream().noneMatch(i -> i.dishId().equals(d))).findFirst().orElseThrow();
        assertThatThrownBy(() -> store.insertItem(batch.id(), new RecommendationModels.StoredItem(
                spare, 1, "EXPLORE", .5, "DAILY_DISCOVERY"))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> store.insertItem(batch.id(), new RecommendationModels.StoredItem(
                spare, 0, "EXPLORE", .5, "DAILY_DISCOVERY"))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void endpointRequiresLoginAndReturnsSameBusinessDataForDifferentUsers() throws Exception {
        catalogue(30);
        dates.add(service.currentDate());
        var anonymous = new Browser();
        assertThat(anonymous.get("/api/v1/recommendations/daily").statusCode()).isEqualTo(401);
        var first = login("user").get("/api/v1/recommendations/daily");
        var second = login("user").get("/api/v1/recommendations/daily?userId=" + UUID.randomUUID());
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(second.statusCode()).isEqualTo(200);
        var data = json.readTree(first.body()).path("data");
        assertThat(data).isEqualTo(json.readTree(second.body()).path("data"));
        assertThat(data.path("total").asInt()).isEqualTo(10);
        assertThat(data.path("algorithmVersion").asString()).isEqualTo("FT-HybridRec-v2");
    }

    @Test void zeroCandidatesProduceSuccessfulEmptySnapshot() {
        LocalDate date = testDate(7);
        var response = service.get(date);
        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isZero();
        assertThat(store.findSuccessfulBatch(date)).isNotNull();
    }

    @Test void shanghaiDateUsesInjectedClockAtUtcBoundary() {
        Clock boundary = Clock.fixed(Instant.parse("2026-11-02T16:01:00Z"), ZoneOffset.UTC);
        var serviceAtBoundary = new DailyRecommendationService(store, cache, config, generator, boundary, transactions);
        assertThat(serviceAtBoundary.currentDate()).isEqualTo(LocalDate.of(2026, 11, 3));
    }
}
