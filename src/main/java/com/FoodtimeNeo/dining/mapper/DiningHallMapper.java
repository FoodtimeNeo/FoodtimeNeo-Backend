package com.FoodtimeNeo.dining.mapper;

import com.FoodtimeNeo.dining.entity.DiningHall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DiningHallMapper {
    @Select("""
            SELECT id, name, cover_image_url, description, latitude, longitude, status, sort_order
            FROM foodtime.dining_halls
            WHERE status = 'active'
            ORDER BY sort_order ASC, id ASC
            """)
    List<DiningHall> findActive();
}
