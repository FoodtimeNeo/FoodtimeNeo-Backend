package com.FoodtimeNeo.dish.mapper;

import com.FoodtimeNeo.dish.entity.DishDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.UUID;

@Mapper
public interface DishDetailMapper {
    @Select("""
            SELECT d.id, s.id AS stall_id, s.name AS stall_name,
                   h.id AS dining_hall_id, h.name AS dining_hall_name, d.name, d.description, d.price
            FROM foodtime.dishes d
            JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
            WHERE d.id = #{dishId,jdbcType=OTHER} AND d.status = 'active'
            """)
    DishDetail findVisible(UUID dishId);
}
