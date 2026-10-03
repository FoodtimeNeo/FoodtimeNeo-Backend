package com.FoodtimeNeo.user.mapper;

import com.FoodtimeNeo.user.entity.NewUser;
import com.FoodtimeNeo.user.entity.LoginAccount;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface UserMapper {
    @Select("SELECT EXISTS (SELECT 1 FROM foodtime.users WHERE lower(btrim(email)) = #{email})")
    boolean existsByEmail(String email);

    /** Returns 0 for an email conflict, including races after the existence check. */
    @Insert("""
            INSERT INTO foodtime.users (id, email, password_hash, display_name, role, status, email_verified_at)
            VALUES (#{id,jdbcType=OTHER}, #{email}, #{passwordHash}, #{displayName}, 'user', 'active', #{emailVerifiedAt})
            ON CONFLICT (lower(btrim(email))) DO NOTHING
            """)
    int insertRegisteredUser(NewUser user);

    @Select("""
            SELECT id, email, password_hash, display_name, role, status, password_changed_at
            FROM foodtime.users WHERE lower(btrim(email)) = #{account}
            """)
    LoginAccount findLoginAccount(String account);

    @Select("""
            SELECT id, email, display_name, role, status, password_changed_at
            FROM foodtime.users WHERE id = #{id,jdbcType=OTHER}
            """)
    UserProfile findProfile(UUID id);

    /** Detect concurrent password changes or disablement before granting a session. */
    @Update("""
            UPDATE foodtime.users SET last_login_at = statement_timestamp()
            WHERE id = #{id,jdbcType=OTHER} AND status = 'active' AND password_hash = #{passwordHash}
            """)
    int recordSuccessfulLogin(@Param("id") UUID id, @Param("passwordHash") String passwordHash);
}
