package com.FoodtimeNeo.dish.mapper;

import com.FoodtimeNeo.dish.dto.DishSearchResponse;
import com.FoodtimeNeo.dish.service.DishSearchQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishSearchMapper {
    /** Kept for favorites/community projections that share the active catalog chain. */
    String COLUMNS = """
            d.id, d.name, d.price, s.id AS stall_id, s.name AS stall_name,
            h.id AS dining_hall_id, h.name AS dining_hall_name
            """;
    String CATALOG = """
            FROM foodtime.dishes d
            JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
            """;
    String FROM = """
            FROM foodtime.dishes d
            JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
            LEFT JOIN foodtime.dish_rating_summary rs ON rs.dish_id = d.id
            """;
    String WHERE = """
            WHERE d.status = 'active'
            <if test="query.q != null and query.q != ''">
              AND strpos(lower(d.name), lower(#{query.q})) &gt; 0
            </if>
            <if test="query.diningHallIds != null and query.diningHallIds.size() > 0">
              AND h.id IN
              <foreach collection="query.diningHallIds" item="hallId" open="(" separator="," close=")">#{hallId}</foreach>
            </if>
            <if test="query.minPrice != null or query.maxPrice != null">
              AND (d.price IS NULL OR (
                <if test="query.minPrice != null">d.price &gt;= #{query.minPrice}</if>
                <if test="query.minPrice != null and query.maxPrice != null">AND</if>
                <if test="query.maxPrice != null">d.price &lt;= #{query.maxPrice}</if>
              ))
            </if>
            <if test="query.minRating != null">
              AND (rs.average_score IS NULL OR rs.average_score &gt;= #{query.minRating})
            </if>
            """;

    @Select("""
            <script>
            SELECT d.id, d.name, d.price, s.id AS stall_id, s.name AS stall_name,
                   h.id AS dining_hall_id, h.name AS dining_hall_name,
                   rs.average_score, COALESCE(rs.review_count, 0) AS review_count
            """ + FROM + WHERE + """
            ORDER BY (CASE WHEN d.price IS NULL OR rs.average_score IS NULL THEN 1 ELSE 0 END),
              <choose>
                <when test="query.sort == 'price_asc'">
                  (CASE WHEN d.price IS NULL THEN 1 ELSE 0 END), d.price ASC, d.id
                </when>
                <when test="query.sort == 'price_desc'">
                  (CASE WHEN d.price IS NULL THEN 1 ELSE 0 END), d.price DESC, d.id
                </when>
                <when test="query.sort == 'rating_desc'">
                  (CASE WHEN rs.average_score IS NULL THEN 1 ELSE 0 END), rs.average_score DESC, d.id
                </when>
                <otherwise>
                  (CASE WHEN #{query.q} = '' THEN 2
                        WHEN lower(d.name) = lower(#{query.q}) THEN 0
                        WHEN strpos(lower(d.name), lower(#{query.q})) = 1 THEN 1 ELSE 2 END),
                  lower(d.name), d.id
                </otherwise>
              </choose>
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<DishSearchResponse> search(@Param("query") DishSearchQuery query,
                                    @Param("limit") int limit, @Param("offset") long offset);

    @Select("<script>SELECT count(*) " + FROM + WHERE + "</script>")
    long count(@Param("query") DishSearchQuery query);
}
