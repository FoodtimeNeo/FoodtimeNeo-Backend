package com.FoodtimeNeo.community.mapper;

import com.FoodtimeNeo.community.dto.CommunityPostResponse;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.sql.Array;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Community posts with denormalized counters; the row is the record itself,
 * approval only flips status (no formal-table copy like catalog reviews).
 */
@Repository
public class CommunityStore {
    private static final String POST_COLUMNS = """
            p.id, p.user_id, p.content, p.like_count, p.comment_count, p.repost_count,
            p.repost_of_id, p.status, p.review_reason, p.created_at,
            u.display_name AS user_display_name,
            (SELECT COALESCE(array_agg(image_url ORDER BY sort_order, id), '{}') FROM foodtime.community_post_images WHERE post_id = p.id) AS images,
            r.content AS repost_content, ru.display_name AS repost_author,
            a.risk_level AS ai_risk_level, a.confidence AS ai_confidence, a.labels AS ai_labels,
            a.reason AS ai_reason, a.checked_at AS ai_checked_at""";
    private static final String POST_JOINS = """
            FROM foodtime.community_posts p JOIN foodtime.users u ON u.id = p.user_id
            LEFT JOIN foodtime.community_posts r ON r.id = p.repost_of_id
            LEFT JOIN foodtime.users ru ON ru.id = r.user_id
            LEFT JOIN foodtime.ai_moderation_results a ON a.submission_type = 'community-post' AND a.submission_id = p.id""";

    private final JdbcTemplate jdbc;
    public CommunityStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public UUID create(UUID userId, String content, UUID repostOfId, List<String> imageUrls) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.community_posts(id,user_id,content,repost_of_id) VALUES (?, ?, ?, ?)",
                id, userId, content, repostOfId);
        for (int i = 0; i < imageUrls.size(); i++) {
            jdbc.update("INSERT INTO foodtime.community_post_images(id,post_id,image_url,sort_order) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID(), id, imageUrls.get(i), i);
        }
        return id;
    }

    public boolean repostTargetVisible(UUID repostOfId) {
        return !jdbc.query("SELECT id FROM foodtime.community_posts WHERE id = ? AND status = 'approved'",
                (r, n) -> r.getObject(1, UUID.class), repostOfId).isEmpty();
    }

    public List<CommunityPostResponse> feed(UUID viewerId, int size, long offset) {
        return jdbc.query("SELECT " + POST_COLUMNS
                + ", EXISTS(SELECT 1 FROM foodtime.community_likes WHERE user_id = ? AND target_type = 'post' AND target_id = p.id) AS liked"
                + ", EXISTS(SELECT 1 FROM foodtime.community_favorites WHERE user_id = ? AND post_id = p.id) AS favorited "
                + POST_JOINS + " WHERE p.status = 'approved' ORDER BY p.created_at DESC, p.id ASC LIMIT ? OFFSET ?",
                (r, n) -> row(r, viewerId), viewerId, viewerId, size, offset);
    }

    public long feedCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.community_posts WHERE status = 'approved'", Long.class);
    }

    public CommunityPostResponse find(UUID id, UUID viewerId) {
        var rows = jdbc.query("SELECT " + POST_COLUMNS
                + ", EXISTS(SELECT 1 FROM foodtime.community_likes WHERE user_id = ? AND target_type = 'post' AND target_id = p.id) AS liked"
                + ", EXISTS(SELECT 1 FROM foodtime.community_favorites WHERE user_id = ? AND post_id = p.id) AS favorited "
                + POST_JOINS + " WHERE p.id = ?",
                (r, n) -> row(r, viewerId), viewerId, viewerId, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<CommunityPostResponse> mine(UUID userId, int size, long offset) {
        return jdbc.query("SELECT " + POST_COLUMNS
                + ", EXISTS(SELECT 1 FROM foodtime.community_likes WHERE user_id = p.user_id AND target_type = 'post' AND target_id = p.id) AS liked"
                + ", EXISTS(SELECT 1 FROM foodtime.community_favorites WHERE user_id = p.user_id AND post_id = p.id) AS favorited "
                + POST_JOINS + " WHERE p.user_id = ? ORDER BY p.created_at DESC, p.id ASC LIMIT ? OFFSET ?",
                (r, n) -> row(r, userId), userId, size, offset);
    }

    public long mineCount(UUID userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.community_posts WHERE user_id = ?", Long.class, userId);
    }

    public int deleteOwned(UUID id, UUID userId) {
        jdbc.update("DELETE FROM foodtime.community_likes WHERE target_type = 'post' AND target_id = ? AND user_id = ?", id, userId);
        jdbc.update("DELETE FROM foodtime.community_likes WHERE target_type = 'comment' AND target_id IN (SELECT id FROM foodtime.community_comments WHERE post_id = ? AND user_id = ?)", id, userId);
        return jdbc.update("DELETE FROM foodtime.community_posts WHERE id = ? AND user_id = ?", id, userId);
    }

    public void bumpRepostOf(UUID postId) {
        jdbc.update("""
                UPDATE foodtime.community_posts SET repost_count = repost_count + 1 WHERE id = (
                    SELECT repost_of_id FROM foodtime.community_posts WHERE id = ?)""",
                postId);
    }

    public record ImageRef(UUID id, String url) { }

    public List<ImageRef> postImages(UUID postId) {
        return jdbc.query("SELECT id, image_url FROM foodtime.community_post_images WHERE post_id = ? ORDER BY sort_order, id",
                (r, n) -> new ImageRef(r.getObject("id", UUID.class), r.getString("image_url")), postId);
    }

    /** Admin queue items reuse the moderation item shape (target = the post itself). */
    public java.util.List<com.FoodtimeNeo.moderation.dto.ModerationItem> queue(String status, int size, long offset) {
        return jdbc.query("""
                SELECT p.id, p.user_id, p.content, p.status, p.review_reason, p.created_at,
                u.display_name AS user_display_name,
                (SELECT image_url FROM foodtime.community_post_images WHERE post_id = p.id ORDER BY sort_order, id LIMIT 1) AS image_url,
                a.risk_level AS ai_risk_level, a.confidence AS ai_confidence, a.labels AS ai_labels,
                a.reason AS ai_reason, a.checked_at AS ai_checked_at
                FROM foodtime.community_posts p JOIN foodtime.users u ON u.id = p.user_id
                LEFT JOIN foodtime.ai_moderation_results a ON a.submission_type = 'community-post' AND a.submission_id = p.id
                WHERE p.status = ? ORDER BY p.created_at DESC, p.id ASC LIMIT ? OFFSET ?""",
                (r, n) -> {
                    java.sql.Array labels = r.getArray("ai_labels");
                    return new com.FoodtimeNeo.moderation.dto.ModerationItem(r.getObject("id", UUID.class), "community-post",
                        r.getObject("id", UUID.class), r.getObject("user_id", UUID.class), r.getString("user_display_name"),
                        null, r.getString("content"), null, r.getString("image_url"), null, r.getString("status"),
                        r.getString("review_reason"), r.getTimestamp("created_at").toInstant(),
                        r.getString("ai_risk_level"), r.getBigDecimal("ai_confidence"),
                        labels == null ? null : List.of((String[]) labels.getArray()),
                        r.getString("ai_reason"), instant(r, "ai_checked_at"));
                }, status, size, offset);
    }

    public long queueCount(String status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.community_posts WHERE status = ?", Long.class, status);
    }

    public UUID lock(UUID id) {
        var rows = jdbc.query("SELECT id FROM foodtime.community_posts WHERE id = ? FOR UPDATE", (r, n) -> r.getObject(1, UUID.class), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<String> imageUrls(UUID postId) {
        return jdbc.query("SELECT image_url FROM foodtime.community_post_images WHERE post_id = ? ORDER BY sort_order, id",
                (r, n) -> r.getString(1), postId);
    }

    private static CommunityPostResponse row(java.sql.ResultSet r, UUID viewerId) throws java.sql.SQLException {
        Array images = r.getArray("images");
        List<String> urls = images == null ? List.of() : List.of((String[]) images.getArray());
        UUID repostOf = r.getObject("repost_of_id", UUID.class);
        CommunityPostResponse.RepostPreview preview = repostOf == null ? null
                : new CommunityPostResponse.RepostPreview(repostOf, r.getString("repost_author"), snippet(r.getString("repost_content")));
        Array labels = r.getArray("ai_labels");
        return new CommunityPostResponse(r.getObject("id", UUID.class), r.getObject("user_id", UUID.class),
                r.getString("user_display_name"), r.getString("content"), urls,
                r.getInt("like_count"), r.getInt("comment_count"), r.getInt("repost_count"),
                repostOf, preview, r.getBoolean("liked"), r.getBoolean("favorited"),
                r.getString("status"), r.getString("review_reason"),
                r.getString("ai_risk_level"), r.getBigDecimal("ai_confidence"),
                labels == null ? null : List.of((String[]) labels.getArray()),
                r.getString("ai_reason"), instant(r, "ai_checked_at"), r.getTimestamp("created_at").toInstant());
    }

    private static String snippet(String content) {
        if (content == null) return null;
        return content.length() <= 140 ? content : content.substring(0, 140) + "…";
    }

    private static Instant instant(java.sql.ResultSet r, String column) throws java.sql.SQLException {
        Timestamp value = r.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
