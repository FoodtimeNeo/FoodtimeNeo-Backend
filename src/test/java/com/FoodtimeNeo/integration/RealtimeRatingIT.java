package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeRatingIT extends IntegrationApiSupport {
    @Autowired PlatformTransactionManager transactions;

    @Test void ratingChangesDeletionAndRepostingKeepExactTotals() {
        UUID first = formal(owner, 3);
        formal(other, 5);
        assertScore(2, "4.00");
        jdbc.update("UPDATE foodtime.dish_reviews SET rating=1 WHERE id=?", first);
        assertScore(2, "3.00");
        jdbc.update("UPDATE foodtime.dish_reviews SET deleted_at=now() WHERE id=?", first);
        assertScore(1, "5.00");
        UUID repost = formal(owner, 2);
        assertThat(repost).isNotEqualTo(first);
        assertScore(2, "3.50");
    }

    @Test void rollbackRestoresBothDetailsAndCounters() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            formal(owner, 5);
            assertScore(1, "5.00");
            tx.setRollbackOnly();
        });
        assertScore(0, null);
    }

    @Test void concurrentNewReviewsDoNotLoseAnIncrement() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Future<?> a = pool.submit(() -> { await(start); formal(owner, 2); });
            Future<?> b = pool.submit(() -> { await(start); formal(other, 5); });
            start.countDown();
            a.get(15, TimeUnit.SECONDS); b.get(15, TimeUnit.SECONDS);
        }
        assertScore(2, "3.50");
    }

    private void assertScore(long count, String average) {
        assertThat(jdbc.queryForObject("SELECT review_count FROM foodtime.dish_rating_summary WHERE dish_id=?", Long.class, dish)).isEqualTo(count);
        BigDecimal score = jdbc.queryForObject("SELECT average_score FROM foodtime.dish_rating_summary WHERE dish_id=?", BigDecimal.class, dish);
        if (average == null) assertThat(score).isNull();
        else assertThat(score).isEqualByComparingTo(average);
    }
    private static void await(CountDownLatch start) {
        try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
}
