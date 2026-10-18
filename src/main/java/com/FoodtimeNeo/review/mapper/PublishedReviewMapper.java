package com.FoodtimeNeo.review.mapper;

import com.FoodtimeNeo.review.dto.DishRatingResponse;
import com.FoodtimeNeo.review.dto.PublishedReviewResponse;
import com.FoodtimeNeo.review.dto.ReviewCreatedResponse;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.UUID;

@Mapper
public interface PublishedReviewMapper {
    @Select("""
            SELECT d.id FROM foodtime.dishes d
            JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
            WHERE d.id = #{id,jdbcType=OTHER} AND d.status = 'active'
            FOR SHARE OF d, s, h
            """)
    UUID lockVisibleDish(UUID id);

    @Select("""
            SELECT r.id, r.dish_id, r.user_id, u.display_name AS user_display_name,
                   r.rating, r.content, r.created_at, r.updated_at,
                   d.name AS dish_name, s.id AS stall_id, s.name AS stall_name,
                   h.id AS dining_hall_id, h.name AS dining_hall_name,
                   NULL AS ai_risk_level, NULL AS ai_confidence, NULL AS ai_labels,
                   NULL AS ai_reason, NULL AS ai_checked_at
            FROM foodtime.dish_reviews r
            JOIN foodtime.users u ON u.id = r.user_id
            JOIN foodtime.dishes d ON d.id = r.dish_id
            JOIN foodtime.stalls s ON s.id = d.stall_id
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
            WHERE r.dish_id = #{dishId,jdbcType=OTHER}
            ORDER BY r.created_at DESC, r.id ASC LIMIT #{size} OFFSET #{offset}
            """)
    List<PublishedReviewResponse> visiblePage(@Param("dishId") UUID dishId, @Param("size") int size,
                                            @Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM foodtime.dish_reviews WHERE dish_id = #{id,jdbcType=OTHER}")
    long countForDish(UUID id);

    @Select("""
            SELECT r.id, r.dish_id, r.user_id, u.display_name AS user_display_name,
                   r.rating, r.content, r.created_at, r.updated_at,
                   d.name AS dish_name, s.id AS stall_id, s.name AS stall_name,
                   h.id AS dining_hall_id, h.name AS dining_hall_name,
                   NULL AS ai_risk_level, NULL AS ai_confidence, NULL AS ai_labels,
                   NULL AS ai_reason, NULL AS ai_checked_at
            FROM foodtime.dish_reviews r
            JOIN foodtime.users u ON u.id = r.user_id
            JOIN foodtime.dishes d ON d.id = r.dish_id
            JOIN foodtime.stalls s ON s.id = d.stall_id
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
            WHERE r.user_id = #{userId,jdbcType=OTHER}
            ORDER BY r.created_at DESC, r.id ASC LIMIT #{size} OFFSET #{offset}
            """)
    List<PublishedReviewResponse> ownedPage(@Param("userId") UUID userId, @Param("size") int size,
                                          @Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM foodtime.dish_reviews WHERE user_id = #{id,jdbcType=OTHER}")
    long countOwned(UUID id);

    @Insert("""
            INSERT INTO foodtime.dish_review_submissions(id, dish_id, user_id, rating, content)
            VALUES (#{id,jdbcType=OTHER}, #{dishId,jdbcType=OTHER}, #{userId,jdbcType=OTHER}, #{rating}, #{content})
            ON CONFLICT (dish_id, user_id) WHERE status = 'pending' DO NOTHING
            """)
    int submit(@Param("id") UUID id, @Param("dishId") UUID dishId, @Param("userId") UUID userId,
               @Param("rating") int rating, @Param("content") String content);

    @Select("""
            SELECT id, dish_id, rating, content, status, submitted_at
            FROM foodtime.dish_review_submissions WHERE id = #{id,jdbcType=OTHER}
            """)
    ReviewCreatedResponse findSubmission(UUID id);

    @Delete("DELETE FROM foodtime.dish_reviews WHERE id = #{id,jdbcType=OTHER} AND user_id = #{userId,jdbcType=OTHER}")
    int deleteOwned(@Param("id") UUID id, @Param("userId") UUID userId);

    @Select("""
            SELECT d.id AS dish_id, v.average_score, COALESCE(v.review_count, 0) AS review_count
            FROM foodtime.dishes d
            LEFT JOIN foodtime.dish_rating_summary v ON v.dish_id = d.id
            WHERE d.id = #{id,jdbcType=OTHER}
            """)
    DishRatingResponse rating(UUID id);
}
