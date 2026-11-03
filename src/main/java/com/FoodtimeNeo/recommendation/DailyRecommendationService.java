package com.FoodtimeNeo.recommendation;

import com.FoodtimeNeo.config.properties.DailyRecommendationProperties;
import com.FoodtimeNeo.recommendation.dto.DailyRecommendationResponse;
import com.FoodtimeNeo.recommendation.dto.RecommendedDishResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class DailyRecommendationService {
    private static final Logger LOG = LoggerFactory.getLogger(DailyRecommendationService.class);
    private final DailyRecommendationStore store;
    private final DailyRecommendationCacheService cache;
    private final DailyRecommendationProperties properties;
    private final DailyRecommendationGenerator generator;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public DailyRecommendationService(DailyRecommendationStore store, DailyRecommendationCacheService cache,
            DailyRecommendationProperties properties, DailyRecommendationGenerator generator,
            Clock clock, PlatformTransactionManager transactionManager) {
        this.store = store;
        this.cache = cache;
        this.properties = properties;
        this.generator = generator;
        this.clock = clock.withZone(ZoneId.of(properties.zone()));
        this.transaction = new TransactionTemplate(transactionManager);
        // Commit before any Redis update, including calls originating within another transaction.
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public LocalDate currentDate() { return LocalDate.now(clock); }

    public DailyRecommendationResponse today() { return get(currentDate()); }

    public DailyRecommendationResponse get(LocalDate date) {
        UUID cachedBatchId = cache.getBatchId(date);
        DailyRecommendationStore.Batch batch = store.findSuccessfulBatch(date);
        if (batch == null) batch = generate(date);
        else if (!batch.id().equals(cachedBatchId)) cacheBatch(batch);
        List<RecommendedDishResponse> items = store.findVisibleItems(batch.id()).stream()
                .map(RecommendedDishResponse::from).toList();
        return new DailyRecommendationResponse(batch.date(), batch.algorithmVersion(), items.size(), items);
    }

    public DailyRecommendationStore.Batch generate(LocalDate date) {
        var started = clock.instant();
        DailyRecommendationStore.Batch batch;
        try {
            batch = transaction.execute(status -> generateInTransaction(date));
        } catch (RuntimeException failure) {
            // The item transaction has rolled back. Never overwrite a concurrent successful retry.
            try {
                transaction.executeWithoutResult(status ->
                        store.recordFailure(date, properties.algorithmVersion(), properties.configHash()));
            } catch (RuntimeException markerFailure) {
                failure.addSuppressed(markerFailure);
            }
            throw failure;
        }
        cacheBatch(batch);
        LOG.info("Daily recommendations date={} count={} algorithm={} elapsedMs={}", date,
                store.findVisibleItems(batch.id()).size(), batch.algorithmVersion(),
                Duration.between(started, clock.instant()).toMillis());
        return batch;
    }

    private DailyRecommendationStore.Batch generateInTransaction(LocalDate date) {
        var batch = store.lockOrCreateBatch(date, properties.algorithmVersion(), properties.configHash());
        if ("success".equals(batch.status())) return batch;
        store.markGenerating(batch.id(), properties.algorithmVersion(), properties.configHash());
        var from = date.minusDays(properties.recentDays()).atStartOfDay(clock.getZone()).toInstant();
        var to = date.atStartOfDay(clock.getZone()).toInstant();
        var history = store.findExposure(date.minusDays(properties.historyDays()), date);
        var candidates = store.findCandidates(from, to).stream().map(candidate -> {
            var exposure = history.get(candidate.id());
            return candidate.withExposure(exposure == null ? 0 : exposure.count(),
                    exposure == null ? null : exposure.lastDate());
        }).toList();
        var selected = generator.generate(candidates, store.findGlobalAverage(), date, properties);
        store.clearItems(batch.id());
        for (int i = 0; i < selected.size(); i++) {
            var item = selected.get(i);
            store.insertItem(batch.id(), new RecommendationModels.StoredItem(item.scored().candidate().id(), i + 1,
                    item.type(), item.scored().score(), item.reasonCode()));
        }
        store.markSuccess(batch.id());
        return store.findSuccessfulBatch(date);
    }

    private void cacheBatch(DailyRecommendationStore.Batch batch) {
        var ids = store.findVisibleItems(batch.id()).stream().map(DailyRecommendationStore.RecommendationRow::dishId).toList();
        cache.put(batch.date(), batch.id(), ids);
    }
}
