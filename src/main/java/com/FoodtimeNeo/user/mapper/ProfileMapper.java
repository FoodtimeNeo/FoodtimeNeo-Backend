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
            SELECT id, email, display_name, role, avatar_url FROM foodtime.users
            WHERE id = #{userId,jdbcType=OTHER} AND status = 'active'
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
