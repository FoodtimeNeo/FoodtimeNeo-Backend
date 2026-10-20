package com.FoodtimeNeo.review.mapper;

import com.FoodtimeNeo.review.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.Instant;
import java.util.*;

@Repository
public class ReviewReplyStore {
    private final JdbcTemplate jdbc;
    public ReviewReplyStore(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public record Stored(UUID id,UUID reviewId,UUID replyToId,UUID userId,String content,String status,Instant createdAt,Instant deletedAt) { }
    private static Instant time(ResultSet r,String name) throws SQLException {return r.getTimestamp(name)==null?null:r.getTimestamp(name).toInstant();}
    private static final RowMapper<Stored> STORED=(r,n)->new Stored(r.getObject("id",UUID.class),r.getObject("review_id",UUID.class),
            r.getObject("reply_to_id",UUID.class),r.getObject("user_id",UUID.class),r.getString("content"),r.getString("status"),time(r,"created_at"),time(r,"deleted_at"));
    private static final String SELECT="""
            SELECT c.*,u.display_name AS user_display_name,t.user_id AS reply_to_user_id,
                   tu.display_name AS reply_to_user_display_name,(t.deleted_at IS NOT NULL) AS reply_to_deleted,
                   (c.user_id=?) AS owned_by_me,d.id AS dish_id,s.id AS stall_id,s.dining_hall_id,
                   d.name AS dish_name,(r.deleted_at IS NOT NULL) AS review_deleted,
                   0::bigint AS like_count,false AS liked_by_me,0::bigint AS counter_version
            FROM foodtime.dish_review_replies c JOIN foodtime.users u ON u.id=c.user_id
            JOIN foodtime.dish_reviews r ON r.id=c.review_id JOIN foodtime.dishes d ON d.id=r.dish_id
            JOIN foodtime.stalls s ON s.id=d.stall_id
            LEFT JOIN foodtime.dish_review_replies t ON t.id=c.reply_to_id AND t.review_id=c.review_id
            LEFT JOIN foodtime.users tu ON tu.id=t.user_id
            """;
    private static final RowMapper<ReviewReplyResponse> ROW=(r,n)->new ReviewReplyResponse(r.getObject("id",UUID.class),
            r.getObject("review_id",UUID.class),r.getObject("reply_to_id",UUID.class),r.getObject("user_id",UUID.class),r.getString("user_display_name"),
            r.getString("content"),r.getString("status"),r.getBoolean("owned_by_me")?r.getString("review_reason"):null,time(r,"reviewed_at"),time(r,"created_at"),time(r,"updated_at"),
            r.getObject("reply_to_user_id",UUID.class),r.getString("reply_to_user_display_name"),r.getBoolean("reply_to_deleted"),r.getBoolean("owned_by_me"),
            r.getObject("dish_id",UUID.class),r.getObject("stall_id",UUID.class),r.getObject("dining_hall_id",UUID.class),r.getString("dish_name"),r.getBoolean("review_deleted"),
            r.getLong("like_count"),r.getBoolean("liked_by_me"),r.getLong("counter_version"));
    public Stored byRequest(UUID user,UUID request) {
        var rows=jdbc.query("SELECT * FROM foodtime.dish_review_replies WHERE user_id=? AND client_request_id=?",STORED,user,request);
        return rows.isEmpty()?null:rows.get(0);
    }
    public Stored find(UUID id,boolean lock) {
        var rows=jdbc.query("SELECT * FROM foodtime.dish_review_replies WHERE id=?"+(lock?" FOR UPDATE":""),STORED,id);
        return rows.isEmpty()?null:rows.get(0);
    }
    public boolean visibleTarget(UUID review,UUID target) {
        return !jdbc.query("SELECT id FROM foodtime.dish_review_replies WHERE id=? AND review_id=? AND status='approved' AND deleted_at IS NULL FOR SHARE",
                (r,n)->r.getObject(1,UUID.class),target,review).isEmpty();
    }
    public boolean insert(UUID id,UUID review,UUID user,CreateReviewReplyRequest body) {
        return jdbc.update("""
                INSERT INTO foodtime.dish_review_replies(id,review_id,reply_to_id,user_id,client_request_id,content)
                VALUES (?,?,?,?,?,?) ON CONFLICT(user_id,client_request_id) DO NOTHING
                """,id,review,body.replyToId(),user,body.clientRequestId(),body.content().strip())>0;
    }
    public List<ReviewReplyResponse> page(UUID review,UUID viewer,int limit,Instant after,UUID afterId) {
        String cursor=after==null?"":" AND (c.created_at,c.id)>(?,?)";
        var args=new ArrayList<Object>(Arrays.asList(viewer,review));
        if(after!=null) {args.add(Timestamp.from(after));args.add(afterId);}args.add(limit);
        return jdbc.query(SELECT+" WHERE c.review_id=? AND c.status='approved' AND c.deleted_at IS NULL"+cursor
                +" ORDER BY c.created_at ASC,c.id ASC LIMIT ?",ROW,args.toArray());
    }
    public long count(UUID review) {return jdbc.queryForObject("SELECT reply_count FROM foodtime.dish_review_stats WHERE review_id=?",Long.class,review);}
    public List<ReviewReplyResponse> mine(UUID user,int size,long offset) {
        return jdbc.query(SELECT+" WHERE c.user_id=? AND c.deleted_at IS NULL ORDER BY c.created_at DESC,c.id ASC LIMIT ? OFFSET ?",ROW,user,user,size,offset);
    }
    public long countMine(UUID user) {return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_review_replies WHERE user_id=? AND deleted_at IS NULL",Long.class,user);}
    public void delete(UUID id) {jdbc.update("UPDATE foodtime.dish_review_replies SET deleted_at=now() WHERE id=? AND deleted_at IS NULL",id);}
}
