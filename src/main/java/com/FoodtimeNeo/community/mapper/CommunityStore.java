package com.FoodtimeNeo.community.mapper;

import com.FoodtimeNeo.community.dto.CommunityCommentResponse;
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
            p.repost_of_id, p.status, p.review_reason, p.created_at, p.title, p.favorite_count, p.review_stage, p.manual_review_reason,
            u.display_name AS user_display_name, u.avatar_url AS user_avatar_url,
            (SELECT COALESCE(array_agg(image_url ORDER BY sort_order, id), '{}') FROM foodtime.community_post_images WHERE post_id = p.id) AS images,
            (SELECT COALESCE(jsonb_agg(jsonb_build_object('id',id,'url',image_url,'width',width,'height',height,'sortOrder',sort_order) ORDER BY sort_order,id),'[]')::text FROM foodtime.community_post_images WHERE post_id=p.id) AS image_details,
            r.content AS repost_content, ru.display_name AS repost_author,
            a.risk_level AS ai_risk_level, a.confidence AS ai_confidence, a.labels AS ai_labels,
            a.reason AS ai_reason, a.checked_at AS ai_checked_at""";
    private static final String POST_JOINS = """
            FROM foodtime.community_posts p JOIN foodtime.users u ON u.id = p.user_id
            LEFT JOIN foodtime.community_posts r ON r.id = p.repost_of_id AND r.status='approved' AND r.deleted_at IS NULL
            LEFT JOIN foodtime.users ru ON ru.id = r.user_id
            LEFT JOIN foodtime.ai_moderation_results a ON a.submission_type = 'community-post' AND a.submission_id = p.id""";

    private final JdbcTemplate jdbc;
    public CommunityStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public UUID request(UUID userId, UUID requestId) {
        return jdbc.query("SELECT id FROM foodtime.community_posts WHERE user_id=? AND client_request_id=?",
                (r,n)->r.getObject(1,UUID.class),userId,requestId).stream().findFirst().orElse(null);
    }

    public boolean sameRequest(UUID id, com.FoodtimeNeo.community.dto.CreatePostRequest body) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT title=? AND content=? AND repost_of_id IS NOT DISTINCT FROM ? FROM foodtime.community_posts WHERE id=?",
                Boolean.class,body.title(),body.content(),body.repostOfId(),id)) && imageUrls(id).equals(body.imageUrls());
    }

    public UUID create(UUID userId, com.FoodtimeNeo.community.dto.CreatePostRequest body,
            List<com.FoodtimeNeo.media.service.ImageStorageService.UploadedImage> images, boolean aiEnabled) {
        UUID id=UUID.randomUUID();
        int inserted=jdbc.update("INSERT INTO foodtime.community_posts(id,user_id,title,content,repost_of_id,client_request_id,review_stage,manual_review_reason) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(user_id,client_request_id) DO NOTHING",
                id,userId,body.title(),body.content(),body.repostOfId(),body.clientRequestId(),aiEnabled?"ai_pending":"manual_pending",aiEnabled?null:"ai_disabled");
        if(inserted==0) return null;
        for(int i=0;i<images.size();i++) {
            var image=images.get(i);
            jdbc.update("INSERT INTO foodtime.community_post_images(id,post_id,image_url,sort_order,width,height) VALUES(?,?,?,?,?,?)",
                    UUID.randomUUID(),id,image.imageUrl(),i,image.width(),image.height());
        }
        return id;
    }

    public void routeToManual(String type, UUID id, String reason) {
        String table=switch(type){case "community-post"->"community_posts";case "community-comment"->"community_comments";default->throw new IllegalArgumentException(type);};
        jdbc.update("UPDATE foodtime."+table+" SET review_stage='manual_pending',manual_review_reason=? WHERE id=? AND status='pending' AND deleted_at IS NULL AND review_stage='ai_pending'",reason,id);
    }

    public boolean aiPending(String type,UUID id){
        String table=type.equals("community-post")?"community_posts":"community_comments";
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM foodtime."+table+" WHERE id=? AND status='pending' AND review_stage='ai_pending' AND deleted_at IS NULL)",Boolean.class,id));
    }

    public boolean repostTargetVisible(UUID repostOfId) {
        return !jdbc.query("SELECT id FROM foodtime.community_posts WHERE id = ? AND status = 'approved' AND deleted_at IS NULL",
                (r, n) -> r.getObject(1, UUID.class), repostOfId).isEmpty();
    }

    public List<CommunityPostResponse> feed(UUID viewerId, int size, long offset) {
        return jdbc.query("SELECT " + POST_COLUMNS
                + ", EXISTS(SELECT 1 FROM foodtime.community_post_likes WHERE user_id = ? AND post_id = p.id) AS liked"
                + ", EXISTS(SELECT 1 FROM foodtime.community_favorites WHERE user_id = ? AND post_id = p.id) AS favorited "
                + POST_JOINS + " WHERE p.status = 'approved' AND p.deleted_at IS NULL ORDER BY p.created_at DESC, p.id DESC LIMIT ? OFFSET ?",
                (r, n) -> row(r, viewerId), viewerId, viewerId, size, offset);
    }

    public long feedCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.community_posts WHERE status = 'approved' AND deleted_at IS NULL", Long.class);
    }

    public CommunityPostResponse find(UUID id, UUID viewerId) {
        var rows = jdbc.query("SELECT " + POST_COLUMNS
                + ", EXISTS(SELECT 1 FROM foodtime.community_post_likes WHERE user_id = ? AND post_id = p.id) AS liked"
                + ", EXISTS(SELECT 1 FROM foodtime.community_favorites WHERE user_id = ? AND post_id = p.id) AS favorited "
                + POST_JOINS + " WHERE p.id = ? AND p.deleted_at IS NULL",
                (r, n) -> row(r, viewerId), viewerId, viewerId, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<CommunityPostResponse> mine(UUID userId, int size, long offset) {
        return jdbc.query("SELECT " + POST_COLUMNS
                + ", EXISTS(SELECT 1 FROM foodtime.community_post_likes WHERE user_id = p.user_id AND post_id = p.id) AS liked"
                + ", EXISTS(SELECT 1 FROM foodtime.community_favorites WHERE user_id = p.user_id AND post_id = p.id) AS favorited "
                + POST_JOINS + " WHERE p.user_id = ? AND p.deleted_at IS NULL ORDER BY p.created_at DESC, p.id DESC LIMIT ? OFFSET ?",
                (r, n) -> row(r, userId), userId, size, offset);
    }

    public long mineCount(UUID userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.community_posts WHERE user_id = ? AND deleted_at IS NULL", Long.class, userId);
    }

    public int deleteOwned(UUID id, UUID userId) {
        return jdbc.update("UPDATE foodtime.community_posts SET deleted_at=now(),deleted_by_user_id=?,delete_reason='作者删除' WHERE id=? AND user_id=? AND deleted_at IS NULL",userId,id,userId);
    }

    public int remove(UUID id,UUID actor,String reason){return jdbc.update("UPDATE foodtime.community_posts SET deleted_at=now(),deleted_by_user_id=?,delete_reason=? WHERE id=? AND deleted_at IS NULL",actor,reason,id);}

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

    /** Called only after the admin detail service checks the caller's role. */
    public String userEmail(UUID userId) {
        return jdbc.queryForObject("SELECT email FROM foodtime.users WHERE id=?",String.class,userId);
    }

    /** Admin queue items reuse the moderation item shape (target = the post itself). */
    public java.util.List<com.FoodtimeNeo.moderation.dto.ModerationItem> queue(String status, int size, long offset) {
        return jdbc.query("""
                SELECT p.id, p.user_id, p.title, p.content, p.status, p.review_reason, p.created_at,
                u.display_name AS user_display_name, u.email AS user_email,
                (SELECT image_url FROM foodtime.community_post_images WHERE post_id = p.id ORDER BY sort_order, id LIMIT 1) AS image_url,
                a.risk_level AS ai_risk_level, a.confidence AS ai_confidence, a.labels AS ai_labels,
                a.reason AS ai_reason, a.checked_at AS ai_checked_at
                FROM foodtime.community_posts p JOIN foodtime.users u ON u.id = p.user_id
                LEFT JOIN foodtime.ai_moderation_results a ON a.submission_type = 'community-post' AND a.submission_id = p.id
                WHERE p.status = ? AND p.deleted_at IS NULL ORDER BY p.created_at DESC, p.id DESC LIMIT ? OFFSET ?""",
                (r, n) -> {
                    java.sql.Array labels = r.getArray("ai_labels");
                    return new com.FoodtimeNeo.moderation.dto.ModerationItem(r.getObject("id", UUID.class), "community-post",
                        r.getObject("id", UUID.class), r.getObject("user_id", UUID.class), r.getString("user_display_name"),
                        r.getString("title"), r.getString("content"), null, r.getString("image_url"), null, r.getString("status"),
                        r.getString("review_reason"), r.getTimestamp("created_at").toInstant(),
                        r.getString("ai_risk_level"), r.getBigDecimal("ai_confidence"),
                        labels == null ? null : List.of((String[]) labels.getArray()),
                        r.getString("ai_reason"), instant(r, "ai_checked_at"),r.getString("user_email"));
                }, status, size, offset);
    }

    public long queueCount(String status) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.community_posts WHERE status = ? AND deleted_at IS NULL", Long.class, status);
    }

    /** Admin queue for community comments, reusing the moderation item shape. */
    public java.util.List<com.FoodtimeNeo.moderation.dto.ModerationItem> queueComments(String status, int size, long offset) {
        return jdbc.query("""
                SELECT c.id, c.post_id, c.user_id, c.content, c.status, c.review_reason, c.created_at,
                u.display_name AS user_display_name, u.email AS user_email,
                a.risk_level AS ai_risk_level, a.confidence AS ai_confidence, a.labels AS ai_labels,
                a.reason AS ai_reason, a.checked_at AS ai_checked_at
                FROM foodtime.community_comments c JOIN foodtime.users u ON u.id = c.user_id
                LEFT JOIN foodtime.ai_moderation_results a ON a.submission_type = 'community-comment' AND a.submission_id = c.id
                WHERE c.status = ? AND c.deleted_at IS NULL AND EXISTS(SELECT 1 FROM foodtime.community_posts p WHERE p.id=c.post_id AND p.status='approved' AND p.deleted_at IS NULL) ORDER BY c.created_at DESC, c.id ASC LIMIT ? OFFSET ?""",
                (r, n) -> {
                    java.sql.Array labels = r.getArray("ai_labels");
                    return new com.FoodtimeNeo.moderation.dto.ModerationItem(r.getObject("id", UUID.class), "community-comment",
                        r.getObject("post_id", UUID.class), r.getObject("user_id", UUID.class), r.getString("user_display_name"),
                        null, r.getString("content"), null, null, null, r.getString("status"),
                        r.getString("review_reason"), r.getTimestamp("created_at").toInstant(),
                        r.getString("ai_risk_level"), r.getBigDecimal("ai_confidence"),
                        labels == null ? null : java.util.List.of((String[]) labels.getArray()),
                        r.getString("ai_reason"), instant(r, "ai_checked_at"),r.getString("user_email"));
                }, status, size, offset);
    }

    public long queueCommentCount(String status) {
        return jdbc.queryForObject("SELECT count(*) FROM foodtime.community_comments c WHERE c.status=? AND c.deleted_at IS NULL AND EXISTS(SELECT 1 FROM foodtime.community_posts p WHERE p.id=c.post_id AND p.status='approved' AND p.deleted_at IS NULL)", Long.class, status);
    }

    public UUID createComment(UUID postId, UUID userId, String content) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.community_comments(id,post_id,user_id,content) VALUES (?, ?, ?, ?)",
                id, postId, userId, content);
        return id;
    }

    public boolean postVisibleForComment(UUID postId) {
        return !jdbc.query("SELECT id FROM foodtime.community_posts WHERE id = ? AND status = 'approved' AND deleted_at IS NULL",
                (r, n) -> r.getObject(1, UUID.class), postId).isEmpty();
    }

    public void bumpCommentCount(UUID postId) {
        jdbc.update("UPDATE foodtime.community_posts SET comment_count = comment_count + 1 WHERE id = ?", postId);
    }

    public void bumpCommentCountByComment(UUID commentId) {
        jdbc.update("""
                UPDATE foodtime.community_posts SET comment_count = comment_count + 1 WHERE id = (
                    SELECT post_id FROM foodtime.community_comments WHERE id = ?)""",
                commentId);
    }

    public List<CommunityCommentResponse> comments(UUID postId, int size, long offset) {
        return new CommunityThreadStore(jdbc).legacy(postId,size,offset);
    }

    public long commentCount(UUID postId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.community_comments WHERE post_id = ? AND status = 'approved' AND deleted_at IS NULL",
                Long.class, postId);
    }

    private String likeTable(String type){return type.equals("post")?"community_post_likes":"community_comment_likes";}
    private String targetColumn(String type){return type.equals("post")?"post_id":"comment_id";}
    public boolean liked(UUID user,String type,UUID id){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM foodtime."+likeTable(type)+" WHERE user_id=? AND "+targetColumn(type)+"=?)",Boolean.class,user,id));}
    public boolean toggleLike(UUID user,String type,UUID id){boolean active=!liked(user,type,id);setLike(user,type,id,active);return active;}
    public void setLike(UUID user,String type,UUID id,boolean active){
        if(active)jdbc.update("INSERT INTO foodtime."+likeTable(type)+"(user_id,"+targetColumn(type)+") VALUES(?,?) ON CONFLICT DO NOTHING",user,id);
        else jdbc.update("DELETE FROM foodtime."+likeTable(type)+" WHERE user_id=? AND "+targetColumn(type)+"=?",user,id);
    }
    public long likeCount(String type,UUID id){return jdbc.queryForObject("SELECT like_count FROM foodtime."+(type.equals("post")?"community_posts":"community_comments")+" WHERE id=?",Long.class,id);}
    public boolean targetVisible(String type,UUID id,UUID viewer){
        UUID post=id;
        if(type.equals("comment"))post=jdbc.query("SELECT post_id FROM foodtime.community_comments WHERE id=?",(r,n)->r.getObject(1,UUID.class),id).stream().findFirst().orElse(null);
        if(post==null||jdbc.query("SELECT id FROM foodtime.community_posts WHERE id=? AND status='approved' AND deleted_at IS NULL FOR UPDATE",(r,n)->r.getObject(1,UUID.class),post).isEmpty())return false;
        return type.equals("post")||!jdbc.query("SELECT id FROM foodtime.community_comments WHERE id=? AND status='approved' AND deleted_at IS NULL FOR UPDATE",(r,n)->r.getObject(1,UUID.class),id).isEmpty();
    }
    public boolean favorited(UUID user,UUID id){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM foodtime.community_favorites WHERE user_id=? AND post_id=?)",Boolean.class,user,id));}
    public boolean toggleFavorite(UUID user,UUID id){boolean active=!favorited(user,id);setFavorite(user,id,active);return active;}
    public void setFavorite(UUID user,UUID id,boolean active){
        if(active)jdbc.update("INSERT INTO foodtime.community_favorites(user_id,post_id) VALUES(?,?) ON CONFLICT DO NOTHING",user,id);
        else jdbc.update("DELETE FROM foodtime.community_favorites WHERE user_id=? AND post_id=?",user,id);
    }
    public long postFavoriteCount(UUID id){return jdbc.queryForObject("SELECT favorite_count FROM foodtime.community_posts WHERE id=?",Long.class,id);}

    public List<CommunityPostResponse> favorites(UUID userId, int size, long offset) {
        return jdbc.query("SELECT " + POST_COLUMNS
                + ", EXISTS(SELECT 1 FROM foodtime.community_post_likes WHERE user_id = ? AND post_id = p.id) AS liked"
                + ", TRUE AS favorited "
                + POST_JOINS
                + " WHERE EXISTS(SELECT 1 FROM foodtime.community_favorites f WHERE f.user_id = ? AND f.post_id = p.id)"
                + " AND p.deleted_at IS NULL AND p.status = 'approved' ORDER BY (SELECT created_at FROM foodtime.community_favorites WHERE user_id=? AND post_id=p.id) DESC,p.id DESC LIMIT ? OFFSET ?",
                (r, n) -> row(r, userId), userId, userId, userId, size, offset);
    }

    public long favoriteCount(UUID userId) {
        return jdbc.queryForObject("SELECT count(*) FROM foodtime.community_favorites f JOIN foodtime.community_posts p ON p.id=f.post_id WHERE f.user_id=? AND p.status='approved' AND p.deleted_at IS NULL",Long.class,userId);
    }

    public UUID lock(UUID id) {
        var rows = jdbc.query("SELECT id FROM foodtime.community_posts WHERE id = ? FOR UPDATE", (r, n) -> r.getObject(1, UUID.class), id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<String> imageUrls(UUID postId) {
        return jdbc.query("SELECT image_url FROM foodtime.community_post_images WHERE post_id = ? ORDER BY sort_order, id",
                (r, n) -> r.getString(1), postId);
    }

    private CommunityPostResponse row(java.sql.ResultSet r, UUID viewerId) throws java.sql.SQLException {
        Array images = r.getArray("images");
        List<String> urls = images == null ? List.of() : List.of((String[]) images.getArray());
        UUID repostOf = r.getObject("repost_of_id", UUID.class);
        CommunityPostResponse.RepostPreview preview = repostOf == null ? null
                : new CommunityPostResponse.RepostPreview(repostOf, r.getString("repost_author"), snippet(r.getString("repost_content")));
        Array labels = r.getArray("ai_labels");
        return new CommunityPostResponse(r.getObject("id", UUID.class), r.getObject("user_id", UUID.class),
                r.getString("user_display_name"), r.getString("content"), urls,
                r.getLong("like_count"), r.getLong("comment_count"), r.getInt("repost_count"),
                repostOf, preview, r.getBoolean("liked"), r.getBoolean("favorited"),
                r.getString("status"), r.getString("review_reason"),
                r.getString("ai_risk_level"), r.getBigDecimal("ai_confidence"),
                labels == null ? null : List.of((String[]) labels.getArray()),
                r.getString("ai_reason"), instant(r, "ai_checked_at"), r.getTimestamp("created_at").toInstant(),
                r.getString("title"),r.getString("user_avatar_url"),readImages(r.getString("image_details")),
                r.getLong("favorite_count"),r.getString("review_stage"),r.getString("manual_review_reason"));
    }

    private List<CommunityPostResponse.PostImage> readImages(String value) {
        return tools.jackson.databind.json.JsonMapper.builder().build().readValue(value,
                new tools.jackson.core.type.TypeReference<List<CommunityPostResponse.PostImage>>(){});
    }

    public List<CommunityPostResponse> feedAfter(UUID viewerId, int size, com.FoodtimeNeo.community.service.CommunityCursor cursor) {
        var args=new ArrayList<Object>(List.of(viewerId,viewerId));
        String condition="";
        if(cursor!=null){ condition=" AND (p.created_at,p.id)<(?,?)";args.add(Timestamp.from(cursor.time()));args.add(cursor.id()); }
        args.add(size);
        return jdbc.query("SELECT "+POST_COLUMNS
                +",EXISTS(SELECT 1 FROM foodtime.community_post_likes WHERE user_id=? AND post_id=p.id) AS liked"
                +",EXISTS(SELECT 1 FROM foodtime.community_favorites WHERE user_id=? AND post_id=p.id) AS favorited "
                +POST_JOINS+" WHERE p.status='approved' AND p.deleted_at IS NULL"+condition+" ORDER BY p.created_at DESC,p.id DESC LIMIT ?",
                (r,n)->row(r,viewerId),args.toArray());
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
