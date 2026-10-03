package com.FoodtimeNeo.user.mapper;

import com.FoodtimeNeo.user.entity.NewUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("SELECT EXISTS (SELECT 1 FROM foodtime.users WHERE lower(btrim(email)) = #{email})")
    boolean existsByEmail(String email);

    /** Returns 0 for an email conflict, including races after the existence check. */
    @Insert("""
            INSERT INTO foodtime.users (id, email, password_hash, display_name, role, status)
            VALUES (#{id,jdbcType=OTHER}, #{email}, #{passwordHash}, #{displayName}, 'user', 'active')
            ON CONFLICT (lower(btrim(email))) DO NOTHING
            """)
    int insertRegisteredUser(NewUser user);
}
