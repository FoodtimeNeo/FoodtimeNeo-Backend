package com.FoodtimeNeo.dish.mapper;

import com.FoodtimeNeo.dish.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface DishMapper {
    /** Read both ancestor visibility and dish data from the same database snapshot. */
    @Select("""
            SELECT s.id AS stall_id, d.id, d.name, d.price, r.average_score, COALESCE(r.review_count, 0) AS review_count,
                   d.price_unit, d.description, i.image_url AS cover_image_url
            FROM foodtime.stalls s
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
            LEFT JOIN foodtime.dishes d ON d.stall_id = s.id AND d.status = 'active'
            LEFT JOIN foodtime.dish_rating_summary r ON r.dish_id = d.id
            LEFT JOIN LATERAL (
                SELECT image_url FROM foodtime.dish_images
                WHERE dish_id = d.id AND status = 'approved'
                ORDER BY created_at DESC, id ASC LIMIT 1
            ) i ON TRUE
            WHERE s.id = #{stallId,jdbcType=OTHER} AND s.status = 'active'
            ORDER BY d.created_at ASC, d.id ASC
            """)
    List<Dish> findActiveByStall(@Param("stallId") UUID stallId);
}
