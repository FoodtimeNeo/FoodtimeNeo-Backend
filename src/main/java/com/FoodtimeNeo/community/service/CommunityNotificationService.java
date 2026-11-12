package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.notification.NotificationCategory;
import com.FoodtimeNeo.notification.NotificationPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Writes community interaction messages into the existing personal inbox.
 * Likes are aggregated per target; comments/replies stay one row per comment.
 */
@Service
public class CommunityNotificationService {
    private static final String INTERACTION_CATEGORIES =
            "'LIKE_POST','LIKE_COMMENT','COMMENT_POST','REPLY_COMMENT'";
    private final JdbcTemplate jdbc;
    private final NotificationPublisher notifications;

    public CommunityNotificationService(JdbcTemplate jdbc, NotificationPublisher notifications) {
        this.jdbc = jdbc;
        this.notifications = notifications;
    }

    /** Recomputes the single like message for a post/comment after a like or unlike. */
    public void likeChanged(UUID actor, String type, UUID targetId, boolean liked) {
        boolean post = "post".equals(type);
        Target target = post ? postTarget(targetId) : commentTarget(targetId);
        if (target == null || target.ownerId().equals(actor)) return;

        String table = post ? "community_post_likes" : "community_comment_likes";
        String column = post ? "post_id" : "comment_id";
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM foodtime." + table + " l WHERE l." + column
                        + "=? AND l.user_id<>?",
                Long.class, targetId, target.ownerId());
        long count = total == null ? 0 : total;
        String category = post ? "LIKE_POST" : "LIKE_COMMENT";
        UUID commentId = post ? null : targetId;
        UUID rootCommentId = post ? null : target.rootCommentId();

        if (count == 0) {
            jdbc.update("""
                    DELETE FROM foodtime.messages
                    WHERE user_id=? AND category=? AND ref_type='community-post'
                      AND ref_id=? AND ref_comment_id IS NOT DISTINCT FROM ?
                    """, target.ownerId(), category, target.postId(), commentId);
            return;
        }

        List<String> names = jdbc.query(
                "SELECT u.display_name FROM foodtime." + table + " l JOIN foodtime.users u ON u.id=l.user_id"
                        + " WHERE l." + column + "=? AND l.user_id<>? ORDER BY l.created_at DESC LIMIT 2",
                (r, n) -> r.getString(1), targetId, target.ownerId());
        String first = names.isEmpty() ? "" : names.get(0);
        String second = names.size() < 2 ? "" : names.get(1);
        String title = post ? "你的食记收到新的赞" : "你的评论收到新的赞";
        String body = CommunityNotificationText.likeBody(first, second, count, post);

        jdbc.update("""
                INSERT INTO foodtime.messages
                    (id,user_id,title,body,category,ref_type,ref_id,ref_comment_id,ref_root_comment_id,actor_count,is_read)
                VALUES (?,?,?,?,?,'community-post',?,?,?,?,FALSE)
                ON CONFLICT DO NOTHING
                """, UUID.randomUUID(), target.ownerId(), title, body, category,
                target.postId(), commentId, rootCommentId, count);
        jdbc.update("""
                UPDATE foodtime.messages
                SET title=?, body=?, actor_count=?, is_read=FALSE, created_at=now()
                WHERE user_id=? AND category=? AND ref_type='community-post'
                  AND ref_id=? AND ref_comment_id IS NOT DISTINCT FROM ?
                """, title, body, count, target.ownerId(), category,
                target.postId(), commentId);
        if (liked) {
            notifications.publishEventOnly(NotificationCategory.INTERACTION, target.ownerId(),
                    UUID.randomUUID(), title, "你的内容收到新的互动，请到社区查看。",
                    "community-post", target.postId(), commentId, rootCommentId);
        }
    }

    /** Called after a community comment is approved. */
    public void commentApproved(UUID commentId) {
        CommentTarget comment = approvedComment(commentId);
        if (comment == null) return;

        UUID recipient;
        if (comment.rootCommentId() == null) {
            recipient = postOwner(comment.postId());
        } else {
            recipient = comment.replyToUserId() == null ? postOwner(comment.postId()) : comment.replyToUserId();
        }
        if (recipient == null || recipient.equals(comment.authorId())) return;

        boolean reply = comment.rootCommentId() != null;
        String title = reply ? "你的评论收到新回复" : "你的食记收到新评论";
        String body = CommunityNotificationText.commentBody(comment.authorName(), comment.content(), reply);
        String category = reply ? "REPLY_COMMENT" : "COMMENT_POST";
        UUID rootCommentId = comment.rootCommentId() == null ? commentId : comment.rootCommentId();

        jdbc.update("""
                INSERT INTO foodtime.messages
                    (id,user_id,title,body,category,ref_type,ref_id,ref_comment_id,ref_root_comment_id,actor_count,is_read)
                VALUES (?,?,?,?,?,'community-post',?,?,?,1,FALSE)
                ON CONFLICT DO NOTHING
                """, UUID.randomUUID(), recipient, title, body, category,
                comment.postId(), commentId, rootCommentId);
        notifications.publishEventOnly(NotificationCategory.INTERACTION, recipient, UUID.randomUUID(),
                title, "你的内容收到新的互动，请到社区查看。", "community-post",
                comment.postId(), commentId, rootCommentId);
    }

    /** 原帖被作者或管理员删除时，帖子下所有互动消息都失去目标。 */
    public void postRemoved(UUID postId) {
        jdbc.update("DELETE FROM foodtime.messages WHERE ref_type='community-post' AND ref_id=?"
                + " AND category IN (" + INTERACTION_CATEGORIES + ")", postId);
    }

    /** 只清理指向该评论本身的消息；其回复仍可能以墓碑/平铺形式可见。 */
    public void commentRemoved(UUID commentId) {
        jdbc.update("DELETE FROM foodtime.messages WHERE ref_type='community-post' AND ref_comment_id=?"
                + " AND category IN (" + INTERACTION_CATEGORIES + ")", commentId);
    }

    private Target postTarget(UUID id) {
        return jdbc.query("""
                SELECT user_id,id FROM foodtime.community_posts
                WHERE id=? AND status='approved' AND deleted_at IS NULL
                """, (r, n) -> new Target(r.getObject(1, UUID.class), r.getObject(2, UUID.class), null), id)
                .stream().findFirst().orElse(null);
    }

    private Target commentTarget(UUID id) {
        return jdbc.query("""
                SELECT c.user_id,c.post_id,c.root_comment_id FROM foodtime.community_comments c
                JOIN foodtime.community_posts p ON p.id=c.post_id
                WHERE c.id=? AND c.status='approved' AND c.deleted_at IS NULL
                  AND p.status='approved' AND p.deleted_at IS NULL
                """, (r, n) -> {
                    UUID root = r.getObject(3, UUID.class);
                    return new Target(r.getObject(1, UUID.class), r.getObject(2, UUID.class),
                            root == null ? id : root);
                }, id)
                .stream().findFirst().orElse(null);
    }

    private CommentTarget approvedComment(UUID id) {
        return jdbc.query("""
                SELECT c.user_id,c.post_id,c.root_comment_id,c.reply_to_user_id,u.display_name,c.content
                FROM foodtime.community_comments c
                JOIN foodtime.users u ON u.id=c.user_id
                JOIN foodtime.community_posts p ON p.id=c.post_id
                WHERE c.id=? AND c.status='approved' AND c.deleted_at IS NULL
                  AND p.status='approved' AND p.deleted_at IS NULL
                """, (r, n) -> new CommentTarget(
                        r.getObject(1, UUID.class), r.getObject(2, UUID.class),
                        r.getObject(3, UUID.class), r.getObject(4, UUID.class),
                        r.getString(5), r.getString(6)), id)
                .stream().findFirst().orElse(null);
    }

    private UUID postOwner(UUID postId) {
        return jdbc.query("""
                SELECT user_id FROM foodtime.community_posts
                WHERE id=? AND status='approved' AND deleted_at IS NULL
                """, (r, n) -> r.getObject(1, UUID.class), postId)
                .stream().findFirst().orElse(null);
    }

    private record Target(UUID ownerId, UUID postId, UUID rootCommentId) { }

    private record CommentTarget(UUID authorId, UUID postId, UUID rootCommentId,
                                 UUID replyToUserId, String authorName, String content) { }
}
