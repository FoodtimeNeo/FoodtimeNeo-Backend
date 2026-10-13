package com.FoodtimeNeo.admin.mapper;

import com.FoodtimeNeo.admin.dto.AdminUserResponse;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class UserAdminStore {
    private final JdbcTemplate jdbc;
    private static final String FILTER = " WHERE strpos(lower(u.email), lower(?)) > 0 OR strpos(lower(u.display_name), lower(?)) > 0";
    private static final RowMapper<AdminUserResponse> ROW = (r,n) -> new AdminUserResponse(
            r.getObject("id",UUID.class),r.getString("email"),r.getString("display_name"),r.getString("avatar_url"),
            r.getString("role"),r.getString("status"),r.getTimestamp("created_at").toInstant(),r.getLong("review_count"));
    public UserAdminStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<AdminUserResponse> page(String q,int size,long offset) {
        return jdbc.query("SELECT u.id,u.email,u.display_name,u.avatar_url,u.role,u.status,u.created_at," +
                "(SELECT count(*) FROM foodtime.dish_reviews r WHERE r.user_id=u.id) AS review_count FROM foodtime.users u" +
                FILTER + " ORDER BY u.created_at DESC,u.id ASC LIMIT ? OFFSET ?",ROW,q,q,size,offset);
    }
    public long count(String q) { return jdbc.queryForObject("SELECT count(*) FROM foodtime.users u" + FILTER,Long.class,q,q); }
    public record Access(UUID id,String role,String status) { }
    /** One deterministic lock order also covers administrators changing each other's access. */
    public List<Access> lock(UUID actor,UUID target) {
        return jdbc.query("SELECT id,role,status FROM foodtime.users WHERE id IN (?, ?) ORDER BY id FOR UPDATE",
                (r,n) -> new Access(r.getObject("id",UUID.class),r.getString("role"),r.getString("status")),actor,target);
    }
    public void update(UUID id,String role,String status) {
        jdbc.update("UPDATE foodtime.users SET role=?,status=? WHERE id=?",role,status,id);
    }
}
