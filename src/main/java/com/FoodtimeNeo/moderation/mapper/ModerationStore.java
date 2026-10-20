package com.FoodtimeNeo.moderation.mapper;

import com.FoodtimeNeo.moderation.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class ModerationStore {
    private static final String ITEMS = """
            SELECT r.id,'review' AS type,r.dish_id AS target_id,r.user_id,u.display_name AS user_display_name,
                   d.name,r.content,
                   NULL::numeric AS price,NULL::text AS image_url,r.rating::integer AS rating,
                   r.status,r.review_reason AS reason,r.submitted_at AS created_at
            FROM foodtime.dish_review_submissions r JOIN foodtime.dishes d ON d.id = r.dish_id
            JOIN foodtime.users u ON u.id = r.user_id
            UNION ALL
            SELECT s.id,'dish',s.stall_id,s.submitted_by_user_id,u.display_name,s.name,s.description,s.price,s.image_url,NULL::integer,s.status,s.review_reason,s.created_at
            FROM foodtime.dish_submissions s JOIN foodtime.users u ON u.id = s.submitted_by_user_id
            UNION ALL
            SELECT s.id,'stall',s.dining_hall_id,s.submitted_by_user_id,u.display_name,s.name,s.description,NULL::numeric,s.cover_image_url,NULL::integer,s.status,s.review_reason,s.created_at
            FROM foodtime.stall_submissions s JOIN foodtime.users u ON u.id = s.submitted_by_user_id
            UNION ALL
            SELECT i.id,'photo',i.dish_id,i.uploaded_by_user_id,u.display_name,d.name,NULL::text,NULL::numeric,i.image_url,NULL::integer,i.status,i.review_reason,i.created_at
            FROM foodtime.dish_images i JOIN foodtime.dishes d ON d.id=i.dish_id
            JOIN foodtime.users u ON u.id = i.uploaded_by_user_id
            """;
    private static final String ITEMS_WITH_AI =
            "SELECT q.*, a.risk_level AS ai_risk_level, a.confidence AS ai_confidence,"
            + " a.labels AS ai_labels, a.reason AS ai_reason, a.checked_at AS ai_checked_at FROM ("
            + ITEMS + ") q LEFT JOIN foodtime.ai_moderation_results a"
            + " ON a.submission_type = q.type AND a.submission_id = q.id";
    private static final RowMapper<ModerationItem> ROW = (r, n) -> new ModerationItem(
            r.getObject("id", UUID.class),r.getString("type"),r.getObject("target_id",UUID.class),
            r.getObject("user_id",UUID.class),r.getString("user_display_name"),r.getString("name"),r.getString("content"),r.getBigDecimal("price"),
            r.getString("image_url"),r.getObject("rating",Integer.class),r.getString("status"),
            r.getString("reason"),r.getTimestamp("created_at").toInstant(),
            r.getString("ai_risk_level"),r.getBigDecimal("ai_confidence"),readLabels(r),
            r.getString("ai_reason"),readInstant(r,"ai_checked_at"));
    private final JdbcTemplate jdbc;
    public ModerationStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static java.util.List<String> readLabels(java.sql.ResultSet result) throws java.sql.SQLException {
        java.sql.Array column = result.getArray("ai_labels");
        if (column == null) return null;
        return java.util.List.of((String[]) column.getArray());
    }

    private static Instant readInstant(java.sql.ResultSet result, String column) throws java.sql.SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    public List<ModerationItem> page(String status, int size, long offset) {
        return jdbc.query("SELECT * FROM (" + ITEMS_WITH_AI + ") q WHERE status = ? ORDER BY created_at DESC,id ASC LIMIT ? OFFSET ?", ROW, status, size, offset);
    }
    public long count(String status) { return jdbc.queryForObject("SELECT COUNT(*) FROM (" + ITEMS + ") q WHERE status = ?", Long.class, status); }

    public ModerationItem lock(String type, UUID id) {
        List<UUID> locked = jdbc.query("SELECT id FROM " + table(type) + " WHERE id = ? FOR UPDATE", (r,n) -> r.getObject(1, UUID.class), id);
        if (locked.isEmpty()) return null;
        if (type.equals("community-post")) return lockCommunityPost(id);
        if (type.equals("community-comment")) return lockCommunityComment(id);
        return jdbc.queryForObject("SELECT * FROM (" + ITEMS_WITH_AI + ") q WHERE type = ? AND id = ?", ROW, type, id);
    }

    /** Community posts live outside the catalog union; lock builds the same item shape. */
    public ModerationItem lockCommunityPost(UUID id) {
        var rows = jdbc.query("""
                SELECT p.id, p.user_id, p.content, p.status, p.review_reason, p.created_at,
                u.display_name AS user_display_name,
                (SELECT image_url FROM foodtime.community_post_images WHERE post_id = p.id ORDER BY sort_order, id LIMIT 1) AS image_url,
                a.risk_level AS ai_risk_level, a.confidence AS ai_confidence, a.labels AS ai_labels,
                a.reason AS ai_reason, a.checked_at AS ai_checked_at
                FROM foodtime.community_posts p JOIN foodtime.users u ON u.id = p.user_id
                LEFT JOIN foodtime.ai_moderation_results a ON a.submission_type = 'community-post' AND a.submission_id = p.id
                WHERE p.id = ? FOR UPDATE OF p
                """, (r, n) -> new ModerationItem(r.getObject("id", UUID.class), "community-post", r.getObject("id", UUID.class),
                r.getObject("user_id", UUID.class), r.getString("user_display_name"), null, r.getString("content"), null,
                r.getString("image_url"), null, r.getString("status"), r.getString("review_reason"),
                r.getTimestamp("created_at").toInstant(), r.getString("ai_risk_level"), r.getBigDecimal("ai_confidence"),
                readLabels(r), r.getString("ai_reason"), readInstant(r, "ai_checked_at")), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Community comment lock for moderation and auto-decision. */
    public ModerationItem lockCommunityComment(UUID id) {
        var rows = jdbc.query("""
                SELECT c.id, c.post_id, c.user_id, c.content, c.status, c.review_reason, c.created_at,
                u.display_name AS user_display_name,
                a.risk_level AS ai_risk_level, a.confidence AS ai_confidence, a.labels AS ai_labels,
                a.reason AS ai_reason, a.checked_at AS ai_checked_at
                FROM foodtime.community_comments c JOIN foodtime.users u ON u.id = c.user_id
                LEFT JOIN foodtime.ai_moderation_results a ON a.submission_type = 'community-comment' AND a.submission_id = c.id
                WHERE c.id = ? FOR UPDATE OF c
                """, (r, n) -> new ModerationItem(r.getObject("id", UUID.class), "community-comment", r.getObject("id", UUID.class),
                r.getObject("user_id", UUID.class), r.getString("user_display_name"), null, r.getString("content"), null,
                null, null, r.getString("status"), r.getString("review_reason"),
                r.getTimestamp("created_at").toInstant(), r.getString("ai_risk_level"), r.getBigDecimal("ai_confidence"),
                readLabels(r), r.getString("ai_reason"), readInstant(r, "ai_checked_at")), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public UUID publishReview(ModerationItem item) {
        return jdbc.queryForObject("""
                INSERT INTO foodtime.dish_reviews(id,dish_id,user_id,rating,content,created_at,submission_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (dish_id,user_id) WHERE deleted_at IS NULL DO UPDATE
                SET rating = EXCLUDED.rating,content = EXCLUDED.content,
                    submission_id = EXCLUDED.submission_id
                RETURNING id
                """, UUID.class, UUID.randomUUID(), item.targetId(), item.userId(), item.rating(), item.content(), Timestamp.from(item.createdAt()), item.id());
    }

    public UUID publishDish(ModerationItem item, BigDecimal price, UUID moderator, String reason) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,description,price) VALUES (?, ?, ?, ?, ?)", id, item.targetId(), item.name(), item.content(), price);
        jdbc.update("INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,reviewed_by_user_id,reviewed_at,review_reason) VALUES (?, ?, ?, ?, 'approved', ?, now(), ?)",
                UUID.randomUUID(), id, item.userId(), item.imageUrl(),moderator,reason);
        return id;
    }

    public UUID publishStall(ModerationItem item) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.stalls(id,dining_hall_id,name,description,cover_image_url) VALUES (?, ?, ?, ?, ?)",
                id, item.targetId(), item.name(), item.content(), item.imageUrl());
        return id;
    }

    public void complete(ModerationItem item, UUID moderator, String aiModel, String status, String reason, UUID approvedId, BigDecimal price) {
        boolean aiReviewed = item.type().equals("review") || item.type().equals("community-post")
                || item.type().equals("community-comment");
        String extra = item.type().equals("dish") ? ", approved_dish_id = ?,price = ?" : item.type().equals("stall") ? ", approved_stall_id = ?" : "";
        String ai = aiReviewed ? ",reviewed_by_ai_model = ?" : "";
        var args = new java.util.ArrayList<Object>(java.util.Arrays.asList(status,moderator));
        if (aiReviewed) args.add(aiModel);
        args.add(reason);
        if (item.type().equals("dish") || item.type().equals("stall")) args.add(approvedId);
        if (item.type().equals("dish")) args.add(price);
        args.add(item.id());
        jdbc.update("UPDATE " + table(item.type()) + " SET status = ?,reviewed_by_user_id = ?" + ai + ",reviewed_at = now(),review_reason = ?" + extra + " WHERE id = ?", args.toArray());
    }

    public void log(ModerationItem item, UUID moderator, String actor, String aiModel, String action, String reason) {
        jdbc.update("""
                INSERT INTO foodtime.content_moderation_logs(id,review_submission_id,dish_submission_id,stall_submission_id,dish_image_id,community_post_id,community_comment_id,moderator_user_id,actor,ai_model,action,reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(),item.type().equals("review") ? item.id() : null,
                item.type().equals("dish") ? item.id() : null,item.type().equals("stall") ? item.id() : null,
                item.type().equals("photo") ? item.id() : null,
                item.type().equals("community-post") ? item.id() : null,
                item.type().equals("community-comment") ? item.id() : null,
                moderator,actor,aiModel,action,reason);
    }

    public List<ModerationLogResponse> logs(int size, long offset) {
        return jdbc.query("""
                SELECT id,COALESCE(review_submission_id,dish_submission_id,stall_submission_id,dish_image_id,community_post_id,community_comment_id) AS submission_id,
                       CASE WHEN review_submission_id IS NOT NULL THEN 'review' WHEN dish_submission_id IS NOT NULL THEN 'dish' WHEN stall_submission_id IS NOT NULL THEN 'stall' WHEN dish_image_id IS NOT NULL THEN 'photo' WHEN community_post_id IS NOT NULL THEN 'community-post' ELSE 'community-comment' END AS type,
                       moderator_user_id,actor,ai_model,action,reason,created_at
                FROM foodtime.content_moderation_logs ORDER BY created_at DESC,id ASC LIMIT ? OFFSET ?
                """, (r,n) -> new ModerationLogResponse(r.getObject("id",UUID.class),r.getObject("submission_id",UUID.class),
                r.getString("type"),r.getObject("moderator_user_id",UUID.class),r.getString("actor"),r.getString("ai_model"),r.getString("action"),r.getString("reason"),r.getTimestamp("created_at").toInstant()), size, offset);
    }
    public long countLogs() { return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs", Long.class); }

    private String table(String type) {
        // Identifiers are selected only from constants; caller data is never interpolated as SQL.
        return switch (type) {
            case "review" -> "foodtime.dish_review_submissions";
            case "dish" -> "foodtime.dish_submissions";
            case "stall" -> "foodtime.stall_submissions";
            case "photo" -> "foodtime.dish_images";
            case "community-post" -> "foodtime.community_posts";
            case "community-comment" -> "foodtime.community_comments";
            default -> throw new IllegalArgumentException("Unknown submission type");
        };
    }
}
