package com.FoodtimeNeo.review.mapper;

import com.FoodtimeNeo.review.entity.ReviewSubmissionPageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface ReviewSubmissionMapper {
    /** Count and rows use the same owner restriction and database snapshot; no public visibility filter. */
    @Select("""
            WITH owned AS (
                SELECT r.id, r.dish_id, d.name AS dish_name, d.stall_id, s.name AS stall_name,
                       h.id AS dining_hall_id, h.name AS dining_hall_name, r.rating, r.content,
                       r.status, r.submitted_at, r.reviewed_at, r.review_reason, r.updated_at
                FROM foodtime.dish_review_submissions r
                JOIN foodtime.dishes d ON d.id = r.dish_id
                JOIN foodtime.stalls s ON s.id = d.stall_id
                JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
                WHERE r.user_id = #{userId,jdbcType=OTHER}
            )
            SELECT p.id, p.dish_id, p.dish_name, p.stall_id, p.stall_name, p.dining_hall_id,
                   p.dining_hall_name, p.rating, p.content, p.status, p.submitted_at,
                   p.reviewed_at, p.review_reason, p.updated_at, c.total
            FROM (SELECT COUNT(*) AS total FROM owned) c
            LEFT JOIN LATERAL (
                SELECT id, dish_id, dish_name, stall_id, stall_name, dining_hall_id, dining_hall_name,
                       rating, content, status, submitted_at, reviewed_at, review_reason, updated_at
                FROM owned ORDER BY submitted_at DESC, id ASC LIMIT #{size} OFFSET #{offset}
            ) p ON TRUE
            ORDER BY p.submitted_at DESC, p.id ASC
            """)
    List<ReviewSubmissionPageRow> findOwnedPage(@Param("userId") UUID userId,
                                              @Param("size") int size, @Param("offset") long offset);
}
