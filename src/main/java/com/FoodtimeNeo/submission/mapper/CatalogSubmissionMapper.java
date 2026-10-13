package com.FoodtimeNeo.submission.mapper;

import com.FoodtimeNeo.submission.dto.*;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.UUID;

@Mapper
public interface CatalogSubmissionMapper {
    @Select("SELECT id FROM foodtime.dining_halls WHERE id = #{id,jdbcType=OTHER} AND status = 'active' FOR SHARE")
    UUID lockHall(UUID id);

    @Select("""
            SELECT s.id FROM foodtime.stalls s
            JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
            WHERE s.id = #{id,jdbcType=OTHER} AND s.status = 'active' FOR SHARE OF s, h
            """)
    UUID lockStall(UUID id);

    @Insert("""
            INSERT INTO foodtime.dish_submissions(id,stall_id,name,description,price,image_url,submitted_by_user_id)
            VALUES (#{id,jdbcType=OTHER},#{stallId,jdbcType=OTHER},#{body.name},#{body.description},#{body.price},#{body.imageUrl},#{userId,jdbcType=OTHER})
            """)
    int submitDish(@Param("id") UUID id, @Param("stallId") UUID stallId, @Param("userId") UUID userId,
                   @Param("body") SubmitDishRequest body);

    @Insert("""
            INSERT INTO foodtime.stall_submissions(id,dining_hall_id,name,description,cover_image_url,submitted_by_user_id)
            VALUES (#{id,jdbcType=OTHER},#{hallId,jdbcType=OTHER},#{body.name},#{body.description},#{body.coverImageUrl},#{userId,jdbcType=OTHER})
            """)
    int submitStall(@Param("id") UUID id, @Param("hallId") UUID hallId, @Param("userId") UUID userId,
                    @Param("body") SubmitStallRequest body);

    @Select("SELECT id,status,created_at FROM foodtime.dish_submissions WHERE id = #{id,jdbcType=OTHER}")
    SubmissionCreatedResponse createdDish(UUID id);
    @Select("SELECT id,status,created_at FROM foodtime.stall_submissions WHERE id = #{id,jdbcType=OTHER}")
    SubmissionCreatedResponse createdStall(UUID id);

    @Select("""
            SELECT r.id, r.dining_hall_id, h.name AS dining_hall_name, r.name, r.description,
                   r.cover_image_url, r.status, r.reviewed_at, r.review_reason, r.approved_stall_id,
                   r.created_at, r.updated_at
            FROM foodtime.stall_submissions r JOIN foodtime.dining_halls h ON h.id = r.dining_hall_id
            WHERE r.submitted_by_user_id = #{userId,jdbcType=OTHER}
            ORDER BY r.created_at DESC,r.id ASC LIMIT #{size} OFFSET #{offset}
            """)
    List<StallSubmissionResponse> ownedStalls(@Param("userId") UUID userId, @Param("size") int size, @Param("offset") long offset);
    @Select("SELECT COUNT(*) FROM foodtime.stall_submissions WHERE submitted_by_user_id = #{id,jdbcType=OTHER}")
    long countStalls(UUID id);
}
