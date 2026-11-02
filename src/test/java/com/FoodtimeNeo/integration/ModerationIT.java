package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ModerationIT extends IntegrationApiSupport {
    @org.springframework.beans.factory.annotation.Autowired
    private com.FoodtimeNeo.moderation.service.ModerationService moderation;
    private static final Path STORAGE = Path.of(System.getProperty("java.io.tmpdir"), "moderation-test-" + UUID.randomUUID());
    @DynamicPropertySource static void directory(DynamicPropertyRegistry properties) { properties.add("app.storage.directory", STORAGE::toString); }

    @Test
    void ordinaryUsersCannotModerateOrReadLogsEvenWithSpoofedRole() throws Exception {
        Browser student = login(owner);
        UUID id = review(student, "private review");
        assertThat(student.call("GET", "/api/v1/admin/moderation", null).statusCode()).isEqualTo(403);
        assertThat(student.call("GET", "/api/v1/admin/moderation/logs", null).statusCode()).isEqualTo(403);
        assertThat(student.call("POST", moderate("review",id), Map.of("action","approve","role","superadmin")).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.dish_review_submissions WHERE id = ?", String.class,id)).isEqualTo("pending");
    }

    @Test
    void queueItemsCarryTheSubmitterDisplayName() throws Exception {
        UUID id = review(login(owner), "named review");
        Browser admin = login(account("admin"));
        var items = body(admin.call("GET", "/api/v1/admin/moderation?status=pending&size=100", null))
                .path("data").path("items");
        var mine = java.util.stream.StreamSupport.stream(items.spliterator(), false)
                .filter(item -> item.path("id").asString().equals(id.toString()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("created submission missing from queue"));
        assertThat(mine.path("userDisplayName").asString()).isEqualTo("integration user");
        assertThat(mine.path("userEmail").asString()).isEqualTo(owner.email());
        assertThat(mine.path("userId").asString()).isEqualTo(owner.id().toString());
        assertThat(mine.path("content").asString()).isEqualTo("named review");
        assertThat(mine.path("createdAt").asString()).isNotBlank();
    }

    @Test
    void queueFiltersTypesOnServerBeforePaginationAndValidatesTheFilter() throws Exception {
        UUID review=review(login(owner),"type filter review");
        UUID submitted=UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dish_submissions(id,stall_id,submitted_by_user_id,name,description,image_url) VALUES (?,?,?,'筛选菜品','筛选正文','/api/v1/media/local-filter')",submitted,stall,owner.id());
        Browser admin=login(account("admin"));
        var filtered=body(admin.call("GET","/api/v1/admin/moderation?status=pending&type=dish&size=1",null)).path("data");
        assertThat(filtered.path("total").asLong()).isGreaterThanOrEqualTo(1);
        for(var item:filtered.path("items"))assertThat(item.path("type").asString()).isEqualTo("dish");
        var reviews=body(admin.call("GET","/api/v1/admin/moderation?status=pending&type=review&size=100",null)).path("data").path("items");
        assertThat(java.util.stream.StreamSupport.stream(reviews.spliterator(),false).anyMatch(row->row.path("id").asString().equals(review.toString()))).isTrue();
        for(var item:reviews)assertThat(item.path("type").asString()).isEqualTo("review");
        assertThat(admin.call("GET","/api/v1/admin/moderation?type=unknown",null).statusCode()).isEqualTo(400);
        assertThat(login(other).call("GET","/api/v1/admin/moderation?type=dish",null).statusCode()).isEqualTo(403);
    }

    @Test
    void logsIncludePublisherAndSubmissionTimeSeparatelyFromDecisionTime() throws Exception {
        UUID id=review(login(owner),"publisher log body");
        var submitted=java.time.Instant.parse("2026-01-01T01:02:03Z");
        jdbc.update("UPDATE foodtime.dish_review_submissions SET submitted_at=? WHERE id=?",Timestamp.from(submitted),id);
        Browser admin=login(account("admin"));
        assertThat(admin.call("POST",moderate("review",id),Map.of("action","approve")).statusCode()).isEqualTo(200);
        var items=body(admin.call("GET","/api/v1/admin/moderation/logs?size=100",null)).path("data").path("items");
        var log=java.util.stream.StreamSupport.stream(items.spliterator(),false)
                .filter(row->row.path("submissionId").asString().equals(id.toString())).findFirst().orElseThrow();
        assertThat(log.path("userId").asString()).isEqualTo(owner.id().toString());
        assertThat(log.path("userEmail").asString()).isEqualTo(owner.email());
        assertThat(log.path("userDisplayName").asString()).isEqualTo("integration user");
        assertThat(log.path("content").asString()).isEqualTo("publisher log body");
        assertThat(java.time.Instant.parse(log.path("submittedAt").asString())).isEqualTo(submitted);
        assertThat(java.time.Instant.parse(log.path("createdAt").asString())).isAfter(submitted);
        var publicRows=login(other).call("GET","/api/v1/dishes/"+dish+"/reviews",null);
        assertThat(publicRows.body()).doesNotContain(owner.email(),"userEmail");
    }

    @Test
    void approvedReviewCopiesSubmissionTimeAndUpdatesTheUniqueFormalReview() throws Exception {
        Browser student = login(owner), admin = login(account("admin"));
        UUID first = review(student,"first");
        Timestamp submitted = jdbc.queryForObject("SELECT submitted_at FROM foodtime.dish_review_submissions WHERE id = ?",Timestamp.class,first);
        var approved = admin.call("POST",moderate("review",first),Map.of("action","approve"));
        assertThat(approved.statusCode()).isEqualTo(200);
        UUID formal = UUID.fromString(body(approved).path("data").path("approvedId").asString());
        assertThat(jdbc.queryForObject("SELECT created_at FROM foodtime.dish_reviews WHERE id = ?",Timestamp.class,formal)).isEqualTo(submitted);
        UUID second = review(student,"replacement");
        var replaced = admin.call("POST",moderate("review",second),Map.of("action","approve"));
        assertThat(body(replaced).path("data").path("approvedId").asString()).isEqualTo(formal.toString());
        assertThat(jdbc.queryForObject("SELECT content FROM foodtime.dish_reviews WHERE id = ?",String.class,formal)).isEqualTo("replacement");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_reviews WHERE dish_id = ? AND user_id = ?",Long.class,dish,owner.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs WHERE moderator_user_id IN (SELECT id FROM foodtime.users WHERE role = 'admin') AND review_submission_id IN (?, ?)",Long.class,first,second)).isEqualTo(2);
    }

    @Test
    void concurrentModeratorsProduceOneApprovalAndOneLog() throws Exception {
        UUID id = review(login(owner),"concurrent");
        Account moderator = account("admin"); Browser first = login(moderator), second = login(moderator);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Future<Integer> a = pool.submit(() -> { start.await(); return first.call("POST",moderate("review",id),Map.of("action","approve")).statusCode(); });
            Future<Integer> b = pool.submit(() -> { start.await(); return second.call("POST",moderate("review",id),Map.of("action","approve")).statusCode(); });
            start.countDown();
            assertThat(java.util.List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs WHERE review_submission_id = ?",Long.class,id)).isEqualTo(1);
    }

    @Test
    void rejectionRequiresReasonAndPreservesPrivateContent() throws Exception {
        Browser student = login(owner), admin = login(account("admin"));
        UUID id = review(student,"reject me");
        assertThat(admin.call("POST",moderate("review",id),Map.of("action","reject","reason"," ")).statusCode()).isEqualTo(400);
        assertThat(admin.call("POST",moderate("review",id),Map.of("action","reject","reason","unrelated")).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT review_reason FROM foodtime.dish_review_submissions WHERE id = ?",String.class,id)).isEqualTo("unrelated");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_reviews WHERE dish_id = ?",Long.class,dish)).isZero();
        assertThat(body(student.call("GET","/api/v1/users/me/review-submissions",null)).path("data").path("items").get(0).path("reviewReason").asString()).isEqualTo("unrelated");
    }

    @Test
    void approvingDishRequiresPriceAndCreatesLinkedApprovedImageAtomically() throws Exception {
        Browser student = login(owner), admin = login(account("admin"));
        String image = image(student);
        var created = student.call("POST","/api/v1/stalls/" + stall + "/submissions",Map.of("name","posted dish","imageUrl",image));
        UUID id = UUID.fromString(body(created).path("data").path("id").asString());
        assertThat(admin.call("POST",moderate("dish",id),Map.of("action","approve")).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dishes WHERE stall_id = ?",Long.class,stall)).isEqualTo(1);
        var approved = admin.call("POST",moderate("dish",id),Map.of("action","approve","price",new java.math.BigDecimal("8.20")));
        assertThat(approved.statusCode()).isEqualTo(200);
        UUID formal = UUID.fromString(body(approved).path("data").path("approvedId").asString());
        assertThat(jdbc.queryForObject("SELECT approved_dish_id FROM foodtime.dish_submissions WHERE id = ?",UUID.class,id)).isEqualTo(formal);
        assertThat(jdbc.queryForObject("SELECT price FROM foodtime.dishes WHERE id = ?",java.math.BigDecimal.class,formal)).isEqualByComparingTo("8.20");
        assertThat(jdbc.queryForObject("SELECT image_url FROM foodtime.dish_images WHERE dish_id = ? AND status = 'approved'",String.class,formal)).isEqualTo(image);
        assertThat(login(other).image(image).statusCode()).isEqualTo(200);
    }

    @Test
    void aiCannotApproveDishUploadsOrDishPhotosEvenWhenConfidenceIsOne() throws Exception {
        Browser student=login(owner);
        String image=image(student);
        var dishSubmission=student.call("POST","/api/v1/stalls/"+stall+"/submissions",Map.of("name","AI cannot publish","imageUrl",image,"price",8));
        UUID dishId=UUID.fromString(body(dishSubmission).path("data").path("id").asString());
        var photoSubmission=student.call("POST",dishPath("/image-submissions"),Map.of("imageUrl",image));
        UUID photoId=UUID.fromString(body(photoSubmission).path("data").path("id").asString());
        var verdict=new com.FoodtimeNeo.ai.dto.AiVerdict("LOW",1.0,java.util.List.of(),"Low risk suggestion","local-test");
        org.assertj.core.api.Assertions.assertThatThrownBy(()->moderation.autoDecide("dish",dishId,verdict,true))
                .isInstanceOf(com.FoodtimeNeo.common.exception.BusinessException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->moderation.autoDecide("photo",photoId,verdict,true))
                .isInstanceOf(com.FoodtimeNeo.common.exception.BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.dish_submissions WHERE id=?",String.class,dishId)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.dish_images WHERE id=?",String.class,photoId)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dishes WHERE stall_id=?",Long.class,stall)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs WHERE dish_submission_id=? OR dish_image_id=?",Long.class,dishId,photoId)).isZero();
    }

    @Test
    void conflictingStallNamesRollBackWithoutApprovalOrLog() throws Exception {
        Browser student = login(owner), admin = login(account("admin"));
        var created = student.call("POST","/api/v1/dining-halls/" + hall + "/submissions",Map.of("name","integration stall","coverImageUrl",image(student)));
        UUID id = UUID.fromString(body(created).path("data").path("id").asString());
        assertThat(admin.call("POST",moderate("stall",id),Map.of("action","approve")).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.stall_submissions WHERE id = ?",String.class,id)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs WHERE stall_submission_id = ?",Long.class,id)).isZero();
        jdbc.update("UPDATE foodtime.stall_submissions SET name = 'unique submitted stall' WHERE id = ?",id);
        assertThat(admin.call("POST",moderate("stall",id),Map.of("action","approve")).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.stalls WHERE dining_hall_id = ?",Long.class,hall)).isEqualTo(2);
    }

    private UUID review(Browser browser,String content) throws Exception {
        var response = browser.call("POST",dishPath("/reviews"),Map.of("rating",4,"content",content));
        assertThat(response.statusCode()).isEqualTo(202);
        return UUID.fromString(body(response).path("data").path("id").asString());
    }
    private String moderate(String type,UUID id) { return "/api/v1/admin/moderation/" + type + "/" + id; }
    private String image(Browser browser) throws Exception {
        var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),"png",out);
        var response = browser.upload(out.toByteArray()); assertThat(response.statusCode()).isEqualTo(201);
        return body(response).path("data").path("imageUrl").asString();
    }
}
