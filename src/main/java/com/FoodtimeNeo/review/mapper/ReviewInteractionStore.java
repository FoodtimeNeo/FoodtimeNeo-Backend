package com.FoodtimeNeo.review.mapper;

import com.FoodtimeNeo.review.dto.ReviewCounters;
import com.FoodtimeNeo.review.dto.ReviewLikeResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class ReviewInteractionStore {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    public ReviewInteractionStore(JdbcTemplate jdbc) { this.jdbc=jdbc; this.named=new NamedParameterJdbcTemplate(jdbc); }
    public Map<UUID,ReviewCounters> counters(List<UUID> ids, UUID viewer) {
        if (ids.isEmpty()) return Map.of();
        var result = new HashMap<UUID,ReviewCounters>();
        named.query("""
                SELECT s.*, (l.user_id IS NOT NULL) AS liked_by_me FROM foodtime.dish_review_stats s
                LEFT JOIN foodtime.dish_review_likes l ON l.review_id=s.review_id AND l.user_id=:viewer
                WHERE s.review_id IN (:ids)
                """,Map.of("viewer",viewer,"ids",ids),r -> {
            result.put(r.getObject("review_id",UUID.class),new ReviewCounters(r.getLong("like_count"),
                    r.getLong("reply_count"),r.getBoolean("liked_by_me"),r.getLong("counter_version")));
        });
        return result;
    }
    public void setLike(UUID id,UUID user,boolean liked) {
        if (liked) jdbc.update("INSERT INTO foodtime.dish_review_likes(review_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING",id,user);
        else jdbc.update("DELETE FROM foodtime.dish_review_likes WHERE review_id=? AND user_id=?",id,user);
    }
    public ReviewLikeResponse state(UUID id,UUID user) {
        return jdbc.queryForObject("""
                SELECT like_count,counter_version,EXISTS(SELECT 1 FROM foodtime.dish_review_likes
                WHERE review_id=s.review_id AND user_id=?) AS liked_by_me
                FROM foodtime.dish_review_stats s WHERE review_id=?
                """,(r,n) -> new ReviewLikeResponse(r.getBoolean("liked_by_me"),r.getLong("like_count"),r.getLong("counter_version")),user,id);
    }
}
