package com.FoodtimeNeo.user.mapper;

import com.FoodtimeNeo.user.dto.ProfileResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.UUID;

@Mapper
public interface ProfileMapper {
    @Select("""
            SELECT u.id, u.email, u.display_name, u.role, u.avatar_url,
                   (SELECT count(*) FROM foodtime.user_follows f WHERE f.follower_id = u.id) AS following_count,
                   (SELECT count(*) FROM foodtime.user_follows f WHERE f.following_id = u.id) AS follower_count,
                   u.school_email,
                   (u.school_email_verified_at IS NOT NULL) AS school_email_verified
            FROM foodtime.users u
            WHERE u.id = #{userId,jdbcType=OTHER} AND u.status = 'active'
            """)
    ProfileResponse current(UUID userId);

    /** Returns 0 when the account was disabled concurrently. */
    @Update("""
            UPDATE foodtime.users SET display_name = #{displayName}, avatar_url = #{avatarUrl,jdbcType=VARCHAR}
            WHERE id = #{userId,jdbcType=OTHER} AND status = 'active'
            """)
    int update(@Param("userId") UUID userId, @Param("displayName") String displayName,
               @Param("avatarUrl") String avatarUrl);
}
