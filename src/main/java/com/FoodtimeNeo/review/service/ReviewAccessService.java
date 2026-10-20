package com.FoodtimeNeo.review.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

/** Shared write lock order: directory, root review, then replies. Caller owns the transaction. */
@Service
public class ReviewAccessService {
    private final JdbcTemplate jdbc;
    public ReviewAccessService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Target(UUID id, UUID dishId, UUID userId, Instant deletedAt) { }

    public Target lock(UUID id, boolean requireVisible, boolean update) {
        var dishes = jdbc.query("SELECT dish_id FROM foodtime.dish_reviews WHERE id=?",
                (r,n) -> r.getObject(1,UUID.class), id);
        if (dishes.isEmpty()) throw missing();
        String visibility = requireVisible ? " AND d.status='active' AND s.status='active' AND h.status='active'" : "";
        var context = jdbc.query("SELECT d.id FROM foodtime.dishes d JOIN foodtime.stalls s ON s.id=d.stall_id"
                + " JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id WHERE d.id=?" + visibility
                + " FOR SHARE OF d,s,h", (r,n) -> r.getObject(1,UUID.class), dishes.get(0));
        if (context.isEmpty()) throw missing();
        var roots = jdbc.query("SELECT id,dish_id,user_id,deleted_at FROM foodtime.dish_reviews WHERE id=?"
                + (update ? " FOR UPDATE" : " FOR SHARE"), (r,n) -> new Target(r.getObject("id",UUID.class),
                        r.getObject("dish_id",UUID.class),r.getObject("user_id",UUID.class),
                        r.getTimestamp("deleted_at")==null ? null : r.getTimestamp("deleted_at").toInstant()),id);
        if (roots.isEmpty() || requireVisible && roots.get(0).deletedAt()!=null) throw missing();
        return roots.get(0);
    }

    public void readable(UUID id) {
        Boolean visible = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM foodtime.dish_reviews r
                JOIN foodtime.dishes d ON d.id=r.dish_id AND d.status='active'
                JOIN foodtime.stalls s ON s.id=d.stall_id AND s.status='active'
                JOIN foodtime.dining_halls h ON h.id=s.dining_hall_id AND h.status='active'
                WHERE r.id=? AND r.deleted_at IS NULL)
                """,Boolean.class,id);
        if (!Boolean.TRUE.equals(visible)) throw missing();
    }
    public static BusinessException missing() {
        return new BusinessException("REVIEW_NOT_FOUND","评论不存在或暂不可用",HttpStatus.NOT_FOUND);
    }
}
