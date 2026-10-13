package com.FoodtimeNeo.dish.mapper;

import com.FoodtimeNeo.dish.entity.DishImagePageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface DishImageMapper {
    /** Parent visibility, approved count and page are read from one database snapshot. */
    @Select("""
            SELECT d.id AS dish_id, i.id, i.image_url, i.created_at, c.total
            FROM foodtime.dishes d
            JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
            CROSS JOIN LATERAL (
                SELECT COUNT(*) AS total FROM foodtime.dish_images
                WHERE dish_id = d.id AND status = 'approved'
            ) c
            LEFT JOIN LATERAL (
                SELECT id, image_url, created_at FROM foodtime.dish_images
                WHERE dish_id = d.id AND status = 'approved'
                ORDER BY created_at DESC, id ASC LIMIT #{size} OFFSET #{offset}
            ) i ON TRUE
            WHERE d.id = #{dishId,jdbcType=OTHER} AND d.status = 'active'
            ORDER BY i.created_at DESC, i.id ASC
            """)
    List<DishImagePageRow> findVisiblePage(@Param("dishId") UUID dishId,
                                         @Param("size") int size, @Param("offset") long offset);
}
