package com.FoodtimeNeo.dish.mapper;

import com.FoodtimeNeo.dish.dto.DishSearchResponse;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.UUID;

@Mapper
public interface DishFavoriteMapper {
    String VISIBLE_FAVORITES = DishSearchMapper.CATALOG + """
            LEFT JOIN foodtime.dish_rating_summary rs ON rs.dish_id=d.id
            JOIN foodtime.dish_favorites f ON f.dish_id=d.id
            WHERE d.status='active' AND f.user_id=#{userId,jdbcType=OTHER}
            """;

    @Select("SELECT " + DishSearchMapper.COLUMNS
            + ", rs.average_score, COALESCE(rs.review_count,0) AS review_count " + VISIBLE_FAVORITES + """
            ORDER BY f.created_at DESC, f.dish_id DESC LIMIT #{limit} OFFSET #{offset}
            """)
    List<DishSearchResponse> list(UUID userId, int limit, long offset);

    @Select("SELECT count(*) " + VISIBLE_FAVORITES)
    long count(UUID userId);

    @Select("SELECT EXISTS(SELECT 1 " + VISIBLE_FAVORITES + " AND d.id=#{dishId,jdbcType=OTHER})")
    boolean saved(UUID userId, UUID dishId);

    @Select(value = """
            INSERT INTO foodtime.dish_favorites(user_id,dish_id)
            SELECT #{userId,jdbcType=OTHER}, d.id
            """ + DishSearchMapper.CATALOG + """
            WHERE d.id=#{dishId,jdbcType=OTHER} AND d.status='active'
            ON CONFLICT (user_id,dish_id) DO UPDATE SET dish_id=excluded.dish_id
            RETURNING dish_id
            """, affectData = true)
    UUID save(UUID userId, UUID dishId);

    @Delete("DELETE FROM foodtime.dish_favorites WHERE user_id=#{userId,jdbcType=OTHER} AND dish_id=#{dishId,jdbcType=OTHER}")
    void remove(UUID userId, UUID dishId);
}
