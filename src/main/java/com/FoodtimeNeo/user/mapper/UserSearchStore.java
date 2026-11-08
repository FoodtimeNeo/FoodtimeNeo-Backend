package com.FoodtimeNeo.user.mapper;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.user.dto.UserSearchResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/** Public profile projection only; LIKE metacharacters in a nickname remain literal. */
@Repository
public class UserSearchStore {
    private final JdbcTemplate jdbc;

    public UserSearchStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<UserSearchResponse> search(UUID viewer, String keyword, PageRequest page) {
        String literal = escape(keyword);
        return jdbc.query("""
                SELECT u.id, u.display_name, u.avatar_url,
                       EXISTS(SELECT 1 FROM foodtime.user_follows f
                              WHERE f.follower_id = ? AND f.following_id = u.id) AS followed_by_me
                FROM foodtime.users u
                WHERE u.status = 'active' AND u.id <> ?
                  AND lower(u.display_name) LIKE lower(?) ESCAPE '!'
                ORDER BY CASE WHEN lower(u.display_name) = lower(?) THEN 0
                              WHEN lower(u.display_name) LIKE lower(?) ESCAPE '!' THEN 1 ELSE 2 END,
                         lower(u.display_name), u.id
                LIMIT ? OFFSET ?
                """, (row, index) -> new UserSearchResponse(row.getObject("id", UUID.class),
                        row.getString("display_name"), row.getString("avatar_url"), row.getBoolean("followed_by_me")),
                viewer, viewer, "%" + literal + "%", keyword, literal + "%", page.size(), page.offset());
    }

    public long count(UUID viewer, String keyword) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.users
                WHERE status = 'active' AND id <> ? AND lower(display_name) LIKE lower(?) ESCAPE '!'
                """, Long.class, viewer, "%" + escape(keyword) + "%");
    }

    private static String escape(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
