package com.FoodtimeNeo.dish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(prefix = "app.rating-summary", name = "refresh-enabled", havingValue = "true", matchIfMissing = true)
public class DishRatingSummaryRefresher {
    private static final Logger LOG = LoggerFactory.getLogger(DishRatingSummaryRefresher.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public DishRatingSummaryRefresher(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Scheduled(fixedDelayString = "${app.rating-summary.refresh-interval:PT24H}",
            initialDelayString = "${app.rating-summary.initial-delay:PT0S}")
    public void refresh() {
        try {
            Boolean refreshed = transactions.execute(status -> {
                // Transaction-scoped locks are released automatically, including on failure.
                Boolean acquired = jdbc.queryForObject("select pg_try_advisory_xact_lock(707001, 1)", Boolean.class);
                if (!Boolean.TRUE.equals(acquired)) {
                    return false;
                }
                jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY foodtime.dish_rating_summary");
                return true;
            });
            if (Boolean.TRUE.equals(refreshed)) {
                LOG.info("Refreshed dish rating summary");
            }
        } catch (RuntimeException exception) {
            LOG.error("Failed to refresh dish rating summary", exception);
        }
    }
}
