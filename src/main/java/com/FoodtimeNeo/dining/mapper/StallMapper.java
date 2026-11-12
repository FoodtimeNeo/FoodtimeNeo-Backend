package com.FoodtimeNeo.dining.mapper;

import com.FoodtimeNeo.dining.entity.Stall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface StallMapper {
    /** Parent visibility and child data come from one statement and the same database snapshot. */
    @Select("""
            SELECT h.id AS dining_hall_id, s.id, s.name, s.description, s.cover_image_url,
                   s.status, s.sort_order,v.average_score,COALESCE(v.review_count,0) AS review_count,COALESCE(s.revision,0) AS revision
            FROM foodtime.dining_halls h
            LEFT JOIN foodtime.stalls s ON s.dining_hall_id = h.id AND s.status = 'active'
            LEFT JOIN foodtime.venue_review_targets t ON t.stall_id=s.id LEFT JOIN foodtime.venue_rating_summary v ON v.target_id=t.id
            WHERE h.id = #{diningHallId,jdbcType=OTHER} AND h.status = 'active'
            ORDER BY s.sort_order ASC, s.id ASC
            """)
    List<Stall> findActiveByDiningHall(@Param("diningHallId") UUID diningHallId);

    /** 单个档口的可见性与数据同快照：档口或所属食堂不可见时返回 null。 */
    @Select("""
            SELECT s.dining_hall_id AS dining_hall_id, s.id, s.name, s.description, s.cover_image_url,
                   s.status, s.sort_order,v.average_score,COALESCE(v.review_count,0) AS review_count,COALESCE(s.revision,0) AS revision
            FROM foodtime.stalls s
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
            LEFT JOIN foodtime.venue_review_targets t ON t.stall_id=s.id LEFT JOIN foodtime.venue_rating_summary v ON v.target_id=t.id
            WHERE s.id = #{stallId,jdbcType=OTHER} AND s.status = 'active'
            """)
    Stall findVisibleById(@Param("stallId") UUID stallId);
}
