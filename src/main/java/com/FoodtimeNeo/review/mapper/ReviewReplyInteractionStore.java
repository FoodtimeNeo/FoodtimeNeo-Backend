package com.FoodtimeNeo.review.mapper;

import com.FoodtimeNeo.review.dto.ReviewLikeResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public class ReviewReplyInteractionStore {
    private final JdbcTemplate jdbc;
    public ReviewReplyInteractionStore(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public void setLike(UUID id,UUID user,boolean liked) {
        if (liked) jdbc.update("INSERT INTO foodtime.dish_review_reply_likes(reply_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING",id,user);
        else jdbc.update("DELETE FROM foodtime.dish_review_reply_likes WHERE reply_id=? AND user_id=?",id,user);
    }
    public ReviewLikeResponse state(UUID id,UUID user) {
        return jdbc.queryForObject("""
                SELECT like_count,counter_version,EXISTS(SELECT 1 FROM foodtime.dish_review_reply_likes
                WHERE reply_id=s.reply_id AND user_id=?) AS liked_by_me
                FROM foodtime.dish_review_reply_stats s WHERE reply_id=?
                """,(r,n) -> new ReviewLikeResponse(r.getBoolean("liked_by_me"),r.getLong("like_count"),r.getLong("counter_version")),user,id);
    }
}
