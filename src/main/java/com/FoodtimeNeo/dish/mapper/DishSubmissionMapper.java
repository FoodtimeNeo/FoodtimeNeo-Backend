package com.FoodtimeNeo.dish.mapper;

import com.FoodtimeNeo.dish.entity.DishSubmissionPageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface DishSubmissionMapper {
    @Select("""
            WITH owned AS (
                SELECT r.id, r.stall_id, s.name AS stall_name, h.id AS dining_hall_id, h.name AS dining_hall_name,
                       r.name, r.description, r.price, r.image_url, r.status, r.reviewed_at, r.review_reason,
                       r.approved_dish_id, r.created_at, r.updated_at
                FROM foodtime.dish_submissions r
                JOIN foodtime.stalls s ON s.id = r.stall_id
                JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
                WHERE r.submitted_by_user_id = #{userId,jdbcType=OTHER}
            )
            SELECT p.id, p.stall_id, p.stall_name, p.dining_hall_id, p.dining_hall_name,
                   p.name, p.description, p.price, p.image_url, p.status, p.reviewed_at, p.review_reason,
                   p.approved_dish_id, p.created_at, p.updated_at, c.total
            FROM (SELECT COUNT(*) AS total FROM owned) c
            LEFT JOIN LATERAL (
                SELECT id, stall_id, stall_name, dining_hall_id, dining_hall_name, name, description, price,
                       image_url, status, reviewed_at, review_reason, approved_dish_id, created_at, updated_at
                FROM owned ORDER BY created_at DESC, id ASC LIMIT #{size} OFFSET #{offset}
            ) p ON TRUE
            ORDER BY p.created_at DESC, p.id ASC
            """)
    List<DishSubmissionPageRow> findOwnedPage(@Param("userId") UUID userId,
                                            @Param("size") int size, @Param("offset") long offset);
}
