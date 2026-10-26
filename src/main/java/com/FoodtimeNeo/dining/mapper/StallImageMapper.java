package com.FoodtimeNeo.dining.mapper;

import com.FoodtimeNeo.dining.entity.StallImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface StallImageMapper {
    /** 单档口图集：调用方已确认档口与食堂可见，仅按排序取图。 */
    @Select("""
            SELECT stall_id, id, image_url, sort_order
            FROM foodtime.stall_images
            WHERE stall_id = #{stallId,jdbcType=OTHER}
            ORDER BY sort_order ASC, id ASC
            """)
    List<StallImage> findByStall(@Param("stallId") UUID stallId);

    /** 食堂下全部启用档口的图集：与档口列表同属展示口径，避免逐档口 N+1 查询。 */
    @Select("""
            SELECT s.id AS stall_id, i.id, i.image_url, i.sort_order
            FROM foodtime.dining_halls h
            JOIN foodtime.stalls s ON s.dining_hall_id = h.id AND s.status = 'active'
            LEFT JOIN foodtime.stall_images i ON i.stall_id = s.id
            WHERE h.id = #{diningHallId,jdbcType=OTHER} AND h.status = 'active'
            ORDER BY s.sort_order ASC, s.id ASC, i.sort_order ASC, i.id ASC
            """)
    List<StallImage> findActiveByDiningHall(@Param("diningHallId") UUID diningHallId);
}
