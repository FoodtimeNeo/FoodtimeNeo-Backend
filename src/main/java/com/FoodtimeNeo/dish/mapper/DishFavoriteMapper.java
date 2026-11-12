package com.FoodtimeNeo.dish.mapper;

import com.FoodtimeNeo.dish.dto.DishSearchResponse;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
    String FILTER = """
            <if test="groupId != null">
              AND EXISTS(
                SELECT 1 FROM foodtime.dish_favorite_group_members m
                JOIN foodtime.favorite_groups g ON g.id=m.group_id
                WHERE m.group_id=#{groupId,jdbcType=OTHER} AND m.owner_id=f.user_id
                  AND m.dish_id=f.dish_id AND g.owner_id=f.user_id AND g.kind='dish')
            </if>
            <if test="unfiled">
              AND NOT EXISTS(
                SELECT 1 FROM foodtime.dish_favorite_group_members m
                WHERE m.owner_id=f.user_id AND m.dish_id=f.dish_id)
            </if>
            """;

    @Select("<script>SELECT " + DishSearchMapper.COLUMNS
            + ", rs.average_score, COALESCE(rs.review_count,0) AS review_count " + VISIBLE_FAVORITES + FILTER + """
            ORDER BY f.created_at DESC, f.dish_id DESC LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<DishSearchResponse> list(@Param("userId") UUID userId, @Param("groupId") UUID groupId,
                                  @Param("unfiled") boolean unfiled,
                                  @Param("limit") int limit, @Param("offset") long offset);

    @Select("<script>SELECT count(*) " + VISIBLE_FAVORITES + FILTER + "</script>")
    long count(@Param("userId") UUID userId, @Param("groupId") UUID groupId,
               @Param("unfiled") boolean unfiled);

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
