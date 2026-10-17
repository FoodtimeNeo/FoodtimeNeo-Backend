package com.FoodtimeNeo.moderation.mapper;

import com.FoodtimeNeo.moderation.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
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
    private static final RowMapper<ModerationItem> ROW = (r, n) -> new ModerationItem(
            r.getObject("id", UUID.class),r.getString("type"),r.getObject("target_id",UUID.class),
            r.getObject("user_id",UUID.class),r.getString("user_display_name"),r.getString("name"),r.getString("content"),r.getBigDecimal("price"),
            r.getString("image_url"),r.getObject("rating",Integer.class),r.getString("status"),
            r.getString("reason"),r.getTimestamp("created_at").toInstant());
    private final JdbcTemplate jdbc;
    public ModerationStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ModerationItem> page(String status, int size, long offset) {
        return jdbc.query("SELECT * FROM (" + ITEMS + ") q WHERE status = ? ORDER BY created_at DESC,id ASC LIMIT ? OFFSET ?", ROW, status, size, offset);
    }
    public long count(String status) { return jdbc.queryForObject("SELECT COUNT(*) FROM (" + ITEMS + ") q WHERE status = ?", Long.class, status); }

    public ModerationItem lock(String type, UUID id) {
        List<UUID> locked = jdbc.query("SELECT id FROM " + table(type) + " WHERE id = ? FOR UPDATE", (r,n) -> r.getObject(1, UUID.class), id);
        if (locked.isEmpty()) return null;
        return jdbc.queryForObject("SELECT * FROM (" + ITEMS + ") q WHERE type = ? AND id = ?", ROW, type, id);
    }

    public UUID publishReview(ModerationItem item) {
        return jdbc.queryForObject("""
                INSERT INTO foodtime.dish_reviews(id,dish_id,user_id,rating,content,created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (dish_id,user_id) DO UPDATE
                SET rating = EXCLUDED.rating,content = EXCLUDED.content,created_at = EXCLUDED.created_at
                RETURNING id
                """, UUID.class, UUID.randomUUID(), item.targetId(), item.userId(), item.rating(), item.content(), Timestamp.from(item.createdAt()));
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

    public void complete(ModerationItem item, UUID moderator, String status, String reason, UUID approvedId, BigDecimal price) {
        String extra = item.type().equals("dish") ? ", approved_dish_id = ?,price = ?" : item.type().equals("stall") ? ", approved_stall_id = ?" : "";
        var args = new java.util.ArrayList<Object>(java.util.Arrays.asList(status,moderator,reason));
        if (item.type().equals("dish") || item.type().equals("stall")) args.add(approvedId);
        if (item.type().equals("dish")) args.add(price);
        args.add(item.id());
        jdbc.update("UPDATE " + table(item.type()) + " SET status = ?,reviewed_by_user_id = ?,reviewed_at = now(),review_reason = ?" + extra + " WHERE id = ?", args.toArray());
    }

    public void log(ModerationItem item, UUID moderator, String action, String reason) {
        jdbc.update("""
                INSERT INTO foodtime.content_moderation_logs(id,review_submission_id,dish_submission_id,stall_submission_id,dish_image_id,moderator_user_id,action,reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(),item.type().equals("review") ? item.id() : null,
                item.type().equals("dish") ? item.id() : null,item.type().equals("stall") ? item.id() : null,
                item.type().equals("photo") ? item.id() : null,moderator,action,reason);
    }

    public List<ModerationLogResponse> logs(int size, long offset) {
        return jdbc.query("""
                SELECT id,COALESCE(review_submission_id,dish_submission_id,stall_submission_id,dish_image_id) AS submission_id,
                       CASE WHEN review_submission_id IS NOT NULL THEN 'review' WHEN dish_submission_id IS NOT NULL THEN 'dish' WHEN stall_submission_id IS NOT NULL THEN 'stall' ELSE 'photo' END AS type,
                       moderator_user_id,action,reason,created_at
                FROM foodtime.content_moderation_logs ORDER BY created_at DESC,id ASC LIMIT ? OFFSET ?
                """, (r,n) -> new ModerationLogResponse(r.getObject("id",UUID.class),r.getObject("submission_id",UUID.class),
                r.getString("type"),r.getObject("moderator_user_id",UUID.class),r.getString("action"),r.getString("reason"),r.getTimestamp("created_at").toInstant()), size, offset);
    }
    public long countLogs() { return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs", Long.class); }

    private String table(String type) {
        // Identifiers are selected only from constants; caller data is never interpolated as SQL.
        return switch (type) {
            case "review" -> "foodtime.dish_review_submissions";
            case "dish" -> "foodtime.dish_submissions";
            case "stall" -> "foodtime.stall_submissions";
            case "photo" -> "foodtime.dish_images";
            default -> throw new IllegalArgumentException("Unknown submission type");
        };
    }
}
