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
            SELECT s.id AS stall_id, d.id, d.name, d.price
            FROM foodtime.stalls s
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
            LEFT JOIN foodtime.dishes d ON d.stall_id = s.id AND d.status = 'active'
            WHERE s.id = #{stallId,jdbcType=OTHER} AND s.status = 'active'
            ORDER BY d.created_at ASC, d.id ASC
            """)
    List<Dish> findActiveByStall(@Param("stallId") UUID stallId);
}
