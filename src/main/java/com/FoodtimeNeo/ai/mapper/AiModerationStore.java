package com.FoodtimeNeo.ai.mapper;

import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;

@Repository
public class AiModerationStore {
    public record PendingReview(UUID id, int rating, String content) { }
    public record PendingPhoto(UUID id, String imageUrl) { }

    private final JdbcTemplate jdbc;
    public AiModerationStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<PendingReview> pendingReviews(int limit) {
        return jdbc.query("""
                SELECT s.id, s.rating, s.content FROM foodtime.dish_review_submissions s
                WHERE s.status = 'pending' AND NOT EXISTS (
                    SELECT 1 FROM foodtime.ai_moderation_results a
                    WHERE a.submission_type = 'review' AND a.submission_id = s.id)
                ORDER BY s.submitted_at ASC, s.id ASC LIMIT ?
                """, (r, n) -> new PendingReview(r.getObject("id", UUID.class), r.getInt("rating"), r.getString("content")), limit);
    }

    public List<PendingPhoto> pendingPhotos(int limit) {
        return jdbc.query("""
                SELECT i.id, i.image_url FROM foodtime.dish_images i
                WHERE i.status = 'pending' AND NOT EXISTS (
                    SELECT 1 FROM foodtime.ai_moderation_results a
                    WHERE a.submission_type = 'photo' AND a.submission_id = i.id)
                ORDER BY i.created_at ASC, i.id ASC LIMIT ?
                """, (r, n) -> new PendingPhoto(r.getObject("id", UUID.class), r.getString("image_url")), limit);
    }

    /** Single pending review without a verdict yet; empty when already diagnosed, decided or gone. */
    public java.util.Optional<PendingReview> pendingReview(java.util.UUID id) {
        var rows = jdbc.query("""
                SELECT s.id, s.rating, s.content FROM foodtime.dish_review_submissions s
                WHERE s.id = ? AND s.status = 'pending' AND NOT EXISTS (
                    SELECT 1 FROM foodtime.ai_moderation_results a
                    WHERE a.submission_type = 'review' AND a.submission_id = s.id)
                """, (r, n) -> new PendingReview(r.getObject("id", java.util.UUID.class), r.getInt("rating"), r.getString("content")), id);
        return rows.stream().findFirst();
    }

    /** Single pending photo without a verdict yet; empty when already diagnosed, decided or gone. */
    public java.util.Optional<PendingPhoto> pendingPhoto(java.util.UUID id) {
        var rows = jdbc.query("""
                SELECT i.id, i.image_url FROM foodtime.dish_images i
                WHERE i.id = ? AND i.status = 'pending' AND NOT EXISTS (
                    SELECT 1 FROM foodtime.ai_moderation_results a
                    WHERE a.submission_type = 'photo' AND a.submission_id = i.id)
                """, (r, n) -> new PendingPhoto(r.getObject("id", java.util.UUID.class), r.getString("image_url")), id);
        return rows.stream().findFirst();
    }

    public record PendingCommunityPost(UUID id, String content) { }

    public java.util.List<PendingCommunityPost> pendingCommunityPosts(int limit) {
        return jdbc.query("""
                SELECT p.id, p.title || E'\n' || p.content AS content FROM foodtime.community_posts p
                WHERE p.status = 'pending' AND p.deleted_at IS NULL AND p.review_stage='ai_pending'
                ORDER BY p.created_at ASC, p.id ASC LIMIT ?
                """, (r, n) -> new PendingCommunityPost(r.getObject("id", java.util.UUID.class), r.getString("content")), limit);
    }

    /** Single pending community post without a verdict yet; empty when already diagnosed, decided or gone. */
    public java.util.Optional<PendingCommunityPost> pendingCommunityPost(java.util.UUID id) {
        var rows = jdbc.query("""
                SELECT p.id, p.title || E'\n' || p.content AS content FROM foodtime.community_posts p
                WHERE p.id = ? AND p.status = 'pending' AND p.deleted_at IS NULL AND p.review_stage='ai_pending'
                """, (r, n) -> new PendingCommunityPost(r.getObject("id", java.util.UUID.class), r.getString("content")), id);
        return rows.stream().findFirst();
    }

    public record PendingCommunityComment(UUID id, String content) { }

    public java.util.List<PendingCommunityComment> pendingCommunityComments(int limit) {
        return jdbc.query("""
                SELECT c.id, c.content FROM foodtime.community_comments c
                WHERE c.status = 'pending' AND c.deleted_at IS NULL AND c.review_stage='ai_pending' AND EXISTS(SELECT 1 FROM foodtime.community_posts p WHERE p.id=c.post_id AND p.status='approved' AND p.deleted_at IS NULL)
                ORDER BY c.created_at ASC, c.id ASC LIMIT ?
                """, (r, n) -> new PendingCommunityComment(r.getObject("id", java.util.UUID.class), r.getString("content")), limit);
    }

    /** Single pending community comment without a verdict yet; empty when already diagnosed, decided or gone. */
    public java.util.Optional<PendingCommunityComment> pendingCommunityComment(java.util.UUID id) {
        var rows = jdbc.query("""
                SELECT c.id, c.content FROM foodtime.community_comments c
                WHERE c.id = ? AND c.status = 'pending' AND c.deleted_at IS NULL AND c.review_stage='ai_pending' AND EXISTS(SELECT 1 FROM foodtime.community_posts p WHERE p.id=c.post_id AND p.status='approved' AND p.deleted_at IS NULL)
                """, (r, n) -> new PendingCommunityComment(r.getObject("id", java.util.UUID.class), r.getString("content")), id);
        return rows.stream().findFirst();
    }

    public record PendingReviewReply(UUID id,String content) { }
    private static final String PENDING_REPLIES="""
            SELECT c.id,c.content FROM foodtime.dish_review_replies c
            JOIN foodtime.dish_reviews r ON r.id=c.review_id AND r.deleted_at IS NULL
            WHERE c.status='pending' AND c.deleted_at IS NULL AND NOT EXISTS (
                SELECT 1 FROM foodtime.ai_moderation_results a
                WHERE a.submission_type='dish-review-reply' AND a.submission_id=c.id)
            """;
    public List<PendingReviewReply> pendingReviewReplies(int limit) {
        return jdbc.query(PENDING_REPLIES+" ORDER BY c.created_at,c.id LIMIT ?",
                (r,n)->new PendingReviewReply(r.getObject("id",UUID.class),r.getString("content")),limit);
    }
    public Optional<PendingReviewReply> pendingReviewReply(UUID id) {
        return jdbc.query(PENDING_REPLIES+" AND c.id=?",(r,n)->new PendingReviewReply(r.getObject("id",UUID.class),r.getString("content")),id).stream().findFirst();
    }

    /** Returns 1 on insert, 0 when another worker already recorded this submission. */
    public int insert(UUID id, String type, UUID submissionId, String risk, BigDecimal confidence,
                      String[] labels, String reason, String model) {
        return jdbc.update("""
                INSERT INTO foodtime.ai_moderation_results(id,submission_type,submission_id,risk_level,confidence,labels,reason,model)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (submission_type, submission_id) DO NOTHING
                """, id, type, submissionId, risk, confidence, labels, reason, model);
    }
}
