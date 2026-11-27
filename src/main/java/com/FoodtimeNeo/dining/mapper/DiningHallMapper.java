package com.FoodtimeNeo.dining.mapper;

import com.FoodtimeNeo.dining.entity.DiningHall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DiningHallMapper {
    @Select("""
            SELECT h.id, h.name, h.cover_image_url, h.description, h.latitude, h.longitude, h.status, h.sort_order,
                   v.average_score, COALESCE(v.review_count, 0) AS review_count, h.revision,
                   COALESCE(sc.stall_count, 0) AS stall_count
            FROM foodtime.dining_halls h
            LEFT JOIN foodtime.venue_review_targets t ON t.dining_hall_id = h.id
            LEFT JOIN foodtime.venue_rating_summary v ON v.target_id = t.id
            LEFT JOIN (
                SELECT dining_hall_id, COUNT(*) AS stall_count
                FROM foodtime.stalls
                WHERE status = 'active'
                GROUP BY dining_hall_id
            ) sc ON sc.dining_hall_id = h.id
            WHERE h.status = 'active'
            ORDER BY h.sort_order ASC, h.id ASC
            """)
    List<DiningHall> findActive();
}
