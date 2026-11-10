package com.FoodtimeNeo.dish.mapper;

import com.FoodtimeNeo.dish.dto.DishSearchResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DishSearchMapper {
    String COLUMNS = """
            d.id, d.name, d.price, s.id AS stall_id, s.name AS stall_name,
            h.id AS dining_hall_id, h.name AS dining_hall_name
            """;
    String CATALOG = """
            FROM foodtime.dishes d
            JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
            """;
    String MATCH = CATALOG + """
            WHERE d.status = 'active' AND strpos(lower(d.name), lower(#{keyword})) > 0
            """;

    @Select("SELECT " + COLUMNS + MATCH + """
            ORDER BY CASE WHEN lower(d.name) = lower(#{keyword}) THEN 0
                          WHEN strpos(lower(d.name), lower(#{keyword})) = 1 THEN 1 ELSE 2 END,
                     lower(d.name), d.id
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<DishSearchResponse> search(String keyword, int limit, long offset);

    @Select("SELECT count(*) " + MATCH)
    long count(String keyword);
}
