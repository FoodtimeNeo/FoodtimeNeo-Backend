package com.FoodtimeNeo.user.mapper;

import com.FoodtimeNeo.user.dto.ProfileResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.UUID;

@Mapper
public interface ProfileMapper {
    @Select("""
            SELECT id, email, display_name, role, avatar_url FROM foodtime.users
            WHERE id = #{userId,jdbcType=OTHER} AND status = 'active'
            """)
    ProfileResponse current(UUID userId);
}
