package com.FoodtimeNeo.recommendation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.FoodtimeNeo.recommendation.RecommendationModels.Candidate;
import static com.FoodtimeNeo.recommendation.RecommendationModels.StoredItem;

@Repository
public class DailyRecommendationStore {
    private final JdbcTemplate jdbc;

    public DailyRecommendationStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Candidate> findCandidates(Instant recentFrom, Instant recentTo) {
        return jdbc.query("""
                WITH dish_reviews AS (
                    SELECT d.stall_id, SUM(ds.review_count)::bigint AS review_count,
                           SUM(ds.rating_sum)::bigint AS rating_sum
                    FROM foodtime.dishes d
                    LEFT JOIN foodtime.dish_stats ds ON ds.dish_id = d.id
                    WHERE d.status = 'active'
                    GROUP BY d.stall_id
                ), venue_reviews AS (
                    SELECT t.stall_id, COALESCE(SUM(vs.review_count), 0)::bigint AS review_count,
                           COALESCE(SUM(vs.rating * vs.review_count), 0)::bigint AS rating_sum
                    FROM foodtime.venue_review_targets t
                    LEFT JOIN foodtime.venue_rating_summary vs ON vs.target_id = t.id
                    WHERE t.stall_id IS NOT NULL
                    GROUP BY t.stall_id
                ), recent_reviews AS (
                    SELECT target.stall_id, SUM(target.review_count)::bigint AS reviews_7d
                    FROM (
                        SELECT d.stall_id, COUNT(*) AS review_count
                        FROM foodtime.dish_reviews r JOIN foodtime.dishes d ON d.id = r.dish_id
                        WHERE d.status = 'active' AND r.deleted_at IS NULL AND r.created_at >= ? AND r.created_at < ?
                        GROUP BY d.stall_id
                        UNION ALL
                        SELECT t.stall_id, COUNT(*) AS review_count
                        FROM foodtime.venue_reviews r
                        JOIN foodtime.venue_review_targets t ON t.id = r.target_id
                        WHERE r.deleted_at IS NULL AND r.current_submission_id IS NOT NULL
                          AND r.first_published_at >= ? AND r.first_published_at < ?
                          AND t.stall_id IS NOT NULL
                        GROUP BY t.stall_id
                    ) target GROUP BY target.stall_id
                )
                SELECT s.id AS stall_id, s.name AS stall_name,
                       h.id AS dining_hall_id, h.name AS dining_hall_name,
                       (COALESCE(dr.review_count, 0) + COALESCE(vr.review_count, 0)) AS review_count,
                       CASE WHEN COALESCE(dr.review_count, 0) + COALESCE(vr.review_count, 0) = 0 THEN NULL
                            ELSE (COALESCE(dr.rating_sum, 0) + COALESCE(vr.rating_sum, 0))::numeric
                                 / (COALESCE(dr.review_count, 0) + COALESCE(vr.review_count, 0)) END AS average_rating,
                       COALESCE(recent.reviews_7d, 0) AS reviews_7d, s.cover_image_url AS image_url
                FROM foodtime.stalls s
                JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
                LEFT JOIN dish_reviews dr ON dr.stall_id = s.id
                LEFT JOIN venue_reviews vr ON vr.stall_id = s.id
                LEFT JOIN recent_reviews recent ON recent.stall_id = s.id
                WHERE s.status = 'active' AND NULLIF(BTRIM(s.cover_image_url), '') IS NOT NULL
                ORDER BY s.id
                """, (rs, row) -> new Candidate(rs.getObject("stall_id", UUID.class), rs.getString("stall_name"),
                rs.getObject("dining_hall_id", UUID.class), rs.getString("dining_hall_name"),
                rs.getObject("stall_id", UUID.class), rs.getString("stall_name"), null,
                rs.getBigDecimal("average_rating"), rs.getLong("review_count"), rs.getLong("reviews_7d"),
                rs.getString("image_url"), 0L, null), Timestamp.from(recentFrom), Timestamp.from(recentTo),
                Timestamp.from(recentFrom), Timestamp.from(recentTo));
    }

    public Map<UUID, Exposure> findExposure(LocalDate fromInclusive, LocalDate toExclusive) {
        return jdbc.query("""
                SELECT i.stall_id, COUNT(*) FILTER (WHERE b.recommendation_date >= ?) AS exposure_count,
                       MAX(b.recommendation_date) AS last_date
                FROM foodtime.daily_recommendation_stall_item i
                JOIN foodtime.daily_recommendation_batch b ON b.id = i.batch_id
                WHERE b.generation_status = 'success'
                  AND b.recommendation_date < ?
                GROUP BY i.stall_id
                """, rs -> {
            Map<UUID, Exposure> result = new HashMap<>();
            while (rs.next()) result.put(rs.getObject("stall_id", UUID.class),
                    new Exposure(rs.getLong("exposure_count"), rs.getObject("last_date", LocalDate.class)));
            return result;
        }, fromInclusive, toExclusive);
    }

    public Batch lockOrCreateBatch(LocalDate date, String algorithmVersion, String configHash) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO foodtime.daily_recommendation_batch
                    (id, recommendation_date, algorithm_version, config_hash, generation_status)
                VALUES (?, ?, ?, ?, 'generating')
                ON CONFLICT (recommendation_date) DO NOTHING
                """, id, date, algorithmVersion, configHash);
        return jdbc.queryForObject("""
                SELECT id, recommendation_date, algorithm_version, config_hash, generation_status,
                       generated_at, created_at, updated_at
                FROM foodtime.daily_recommendation_batch
                WHERE recommendation_date = ? FOR UPDATE
                """, (rs, row) -> new Batch(rs.getObject("id", UUID.class),
                rs.getObject("recommendation_date", LocalDate.class), rs.getString("algorithm_version"),
                rs.getString("config_hash"), rs.getString("generation_status"),
                timestamp(rs.getTimestamp("generated_at"))), date);
    }

    public void markGenerating(UUID batchId, String algorithmVersion, String configHash) {
        jdbc.update("""
                UPDATE foodtime.daily_recommendation_batch
                SET algorithm_version = ?, config_hash = ?, generation_status = 'generating', generated_at = NULL
                WHERE id = ?
                """, algorithmVersion, configHash, batchId);
    }

    public void clearItems(UUID batchId) {
        jdbc.update("DELETE FROM foodtime.daily_recommendation_stall_item WHERE batch_id = ?", batchId);
    }

    public void insertItem(UUID batchId, StoredItem item) {
        jdbc.update("""
                INSERT INTO foodtime.daily_recommendation_stall_item
                    (id, batch_id, stall_id, position, recommendation_type, recommendation_score, reason_code)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), batchId, item.stallId(), item.position(), item.type(), item.score(), item.reasonCode());
    }

    public void markSuccess(UUID batchId) {
        jdbc.update("""
                UPDATE foodtime.daily_recommendation_batch
                SET generation_status = 'success', generated_at = now()
                WHERE id = ?
                """, batchId);
    }

    public double findGlobalAverage() {
        return jdbc.queryForObject("""
                WITH ratings AS (
                    SELECT rating_sum, review_count FROM foodtime.dish_stats
                    UNION ALL
                    SELECT SUM(rating * review_count), SUM(review_count)
                    FROM foodtime.venue_rating_stats
                    GROUP BY target_id
                )
                SELECT COALESCE(SUM(rating_sum)::numeric / NULLIF(SUM(review_count), 0), 3.5) FROM ratings
                """, Double.class);
    }

    public void recordFailure(LocalDate date, String version, String configHash) {
        jdbc.update("""
                INSERT INTO foodtime.daily_recommendation_batch
                    (id, recommendation_date, algorithm_version, config_hash, generation_status)
                VALUES (?, ?, ?, ?, 'failed')
                ON CONFLICT (recommendation_date) DO UPDATE
                SET generation_status = 'failed', generated_at = NULL
                WHERE daily_recommendation_batch.generation_status <> 'success'
                """, UUID.randomUUID(), date, version, configHash);
    }

    public Batch findSuccessfulBatch(LocalDate date) {
        List<Batch> rows = jdbc.query("""
                SELECT id, recommendation_date, algorithm_version, config_hash, generation_status, generated_at
                FROM foodtime.daily_recommendation_batch
                WHERE recommendation_date = ? AND generation_status = 'success'
                """, (rs, row) -> new Batch(rs.getObject("id", UUID.class),
                rs.getObject("recommendation_date", LocalDate.class), rs.getString("algorithm_version"),
                rs.getString("config_hash"), rs.getString("generation_status"),
                timestamp(rs.getTimestamp("generated_at"))), date);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<RecommendationRow> findVisibleItems(UUID batchId) {
        return jdbc.query("""
                SELECT i.stall_id, i.position, i.recommendation_type, i.recommendation_score, i.reason_code,
                       s.name AS stall_name,
                       h.id AS dining_hall_id, h.name AS dining_hall_name,
                       (COALESCE(dr.review_count, 0) + COALESCE(vr.review_count, 0)) AS review_count,
                       CASE WHEN COALESCE(dr.review_count, 0) + COALESCE(vr.review_count, 0) = 0 THEN NULL
                            ELSE ROUND((COALESCE(dr.rating_sum, 0) + COALESCE(vr.rating_sum, 0))::numeric
                                 / (COALESCE(dr.review_count, 0) + COALESCE(vr.review_count, 0)), 2) END AS average_rating,
                       s.cover_image_url AS image_url
                FROM foodtime.daily_recommendation_stall_item i
                JOIN foodtime.daily_recommendation_batch b
                  ON b.id = i.batch_id AND b.generation_status = 'success'
                JOIN foodtime.stalls s ON s.id = i.stall_id AND s.status = 'active'
                JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
                LEFT JOIN LATERAL (
                    SELECT SUM(ds.review_count)::bigint AS review_count, SUM(ds.rating_sum)::bigint AS rating_sum
                    FROM foodtime.dishes d LEFT JOIN foodtime.dish_stats ds ON ds.dish_id = d.id
                    WHERE d.stall_id = s.id AND d.status = 'active'
                ) dr ON TRUE
                LEFT JOIN LATERAL (
                    SELECT COALESCE(SUM(vs.review_count), 0)::bigint AS review_count,
                           COALESCE(SUM(vs.rating * vs.review_count), 0)::bigint AS rating_sum
                    FROM foodtime.venue_review_targets t LEFT JOIN foodtime.venue_rating_summary vs ON vs.target_id=t.id
                    WHERE t.stall_id=s.id
                ) vr ON TRUE
                WHERE i.batch_id = ? AND NULLIF(BTRIM(s.cover_image_url), '') IS NOT NULL
                ORDER BY i.position
                """, (rs, row) -> new RecommendationRow(
                rs.getObject("stall_id", UUID.class), rs.getInt("position"), rs.getString("recommendation_type"),
                rs.getDouble("recommendation_score"), rs.getString("reason_code"), rs.getString("stall_name"),
                rs.getObject("dining_hall_id", UUID.class), rs.getString("dining_hall_name"),
                rs.getBigDecimal("average_rating"), rs.getLong("review_count"), rs.getString("image_url")), batchId);
    }

    private static Instant timestamp(Timestamp value) { return value == null ? null : value.toInstant(); }

    public record Exposure(long count, LocalDate lastDate) { }
    public record Batch(UUID id, LocalDate date, String algorithmVersion, String configHash,
                        String status, Instant generatedAt) { }
    public record RecommendationRow(UUID stallId, int position, String type, double score, String reasonCode,
                                    String stallName, UUID diningHallId, String diningHallName,
                                    BigDecimal averageRating, long reviewCount, String imageUrl) { }
}
