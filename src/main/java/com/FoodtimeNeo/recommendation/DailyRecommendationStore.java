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
                SELECT d.id AS dish_id, d.name, d.price,
                       s.id AS stall_id, s.name AS stall_name,
                       h.id AS dining_hall_id, h.name AS dining_hall_name,
                       COALESCE(ds.review_count, 0) AS review_count,
                       CASE WHEN COALESCE(ds.review_count, 0) = 0 THEN NULL
                            ELSE ds.rating_sum::numeric / ds.review_count END AS average_rating,
                       COALESCE(recent.reviews_7d, 0) AS reviews_7d,
                       image.image_url
                FROM foodtime.dishes d
                JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
                JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
                LEFT JOIN foodtime.dish_stats ds ON ds.dish_id = d.id
                LEFT JOIN (
                    SELECT r.dish_id, COUNT(*) AS reviews_7d
                    FROM foodtime.dish_reviews r
                    WHERE r.deleted_at IS NULL AND r.created_at >= ? AND r.created_at < ?
                    GROUP BY r.dish_id
                ) recent ON recent.dish_id = d.id
                LEFT JOIN LATERAL (
                    SELECT i.image_url FROM foodtime.dish_images i
                    WHERE i.dish_id = d.id AND i.status = 'approved'
                    ORDER BY i.created_at DESC, i.id ASC LIMIT 1
                ) image ON TRUE
                WHERE d.status = 'active'
                ORDER BY d.id
                """, (rs, row) -> new Candidate(rs.getObject("dish_id", UUID.class), rs.getString("name"),
                rs.getObject("dining_hall_id", UUID.class), rs.getString("dining_hall_name"),
                rs.getObject("stall_id", UUID.class), rs.getString("stall_name"), rs.getBigDecimal("price"),
                rs.getBigDecimal("average_rating"), rs.getLong("review_count"), rs.getLong("reviews_7d"),
                rs.getString("image_url"), 0L, null), Timestamp.from(recentFrom), Timestamp.from(recentTo));
    }

    public Map<UUID, Exposure> findExposure(LocalDate fromInclusive, LocalDate toExclusive) {
        return jdbc.query("""
                SELECT i.dish_id, COUNT(*) FILTER (WHERE b.recommendation_date >= ?) AS exposure_count,
                       MAX(b.recommendation_date) AS last_date
                FROM foodtime.daily_recommendation_item i
                JOIN foodtime.daily_recommendation_batch b ON b.id = i.batch_id
                WHERE b.generation_status = 'success'
                  AND b.recommendation_date < ?
                GROUP BY i.dish_id
                """, rs -> {
            Map<UUID, Exposure> result = new HashMap<>();
            while (rs.next()) result.put(rs.getObject("dish_id", UUID.class),
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
        jdbc.update("DELETE FROM foodtime.daily_recommendation_item WHERE batch_id = ?", batchId);
    }

    public void insertItem(UUID batchId, StoredItem item) {
        jdbc.update("""
                INSERT INTO foodtime.daily_recommendation_item
                    (id, batch_id, dish_id, position, recommendation_type, recommendation_score, reason_code)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), batchId, item.dishId(), item.position(), item.type(), item.score(), item.reasonCode());
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
                SELECT COALESCE(SUM(rating_sum)::numeric / NULLIF(SUM(review_count), 0), 3.5)
                FROM foodtime.dish_stats
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
                SELECT i.dish_id, i.position, i.recommendation_type, i.recommendation_score, i.reason_code,
                       d.name, d.price, s.id AS stall_id, s.name AS stall_name,
                       h.id AS dining_hall_id, h.name AS dining_hall_name,
                       COALESCE(ds.review_count, 0) AS review_count,
                       CASE WHEN COALESCE(ds.review_count, 0) = 0 THEN NULL
                            ELSE ROUND(ds.rating_sum::numeric / ds.review_count, 2) END AS average_rating,
                       image.image_url
                FROM foodtime.daily_recommendation_item i
                JOIN foodtime.daily_recommendation_batch b
                  ON b.id = i.batch_id AND b.generation_status = 'success'
                JOIN foodtime.dishes d ON d.id = i.dish_id AND d.status = 'active'
                JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
                JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
                LEFT JOIN foodtime.dish_stats ds ON ds.dish_id = d.id
                LEFT JOIN LATERAL (
                    SELECT image_url FROM foodtime.dish_images
                    WHERE dish_id = d.id AND status = 'approved'
                    ORDER BY created_at DESC, id ASC LIMIT 1
                ) image ON TRUE
                WHERE i.batch_id = ?
                ORDER BY i.position
                """, (rs, row) -> new RecommendationRow(
                rs.getObject("dish_id", UUID.class), rs.getInt("position"), rs.getString("recommendation_type"),
                rs.getDouble("recommendation_score"), rs.getString("reason_code"), rs.getString("name"),
                rs.getObject("dining_hall_id", UUID.class), rs.getString("dining_hall_name"),
                rs.getObject("stall_id", UUID.class), rs.getString("stall_name"), rs.getBigDecimal("price"),
                rs.getBigDecimal("average_rating"), rs.getLong("review_count"), rs.getString("image_url")), batchId);
    }

    private static Instant timestamp(Timestamp value) { return value == null ? null : value.toInstant(); }

    public record Exposure(long count, LocalDate lastDate) { }
    public record Batch(UUID id, LocalDate date, String algorithmVersion, String configHash,
                        String status, Instant generatedAt) { }
    public record RecommendationRow(UUID dishId, int position, String type, double score, String reasonCode,
                                    String dishName, UUID diningHallId, String diningHallName,
                                    UUID stallId, String stallName, BigDecimal price, BigDecimal averageRating,
                                    long reviewCount, String imageUrl) { }
}
