package com.FoodtimeNeo.follow.mapper;

import com.FoodtimeNeo.follow.FollowDtos.FollowUserResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** JdbcTemplate access for the directed follow table; every statement is parameterised. */
@Repository
public class FollowStore {
    private static final RowMapper<FollowUserResponse> ROW = (result, index) -> new FollowUserResponse(
            result.getObject("id", UUID.class), result.getString("display_name"),
            result.getString("avatar_url"), result.getBoolean("followed_by_me"));

    private final JdbcTemplate jdbc;

    public FollowStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean targetActive(UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM foodtime.users WHERE id = ? AND status = 'active')",
                Boolean.class, userId));
    }

    public boolean follows(UUID follower, UUID following) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM foodtime.user_follows WHERE follower_id = ? AND following_id = ?)",
                Boolean.class, follower, following));
    }

    /** Idempotent: repeating a follow keeps a single row and the original timestamp. */
    public int follow(UUID follower, UUID following) {
        return jdbc.update("""
                INSERT INTO foodtime.user_follows(follower_id, following_id) VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """, follower, following);
    }

    public int unfollow(UUID follower, UUID following) {
        return jdbc.update(
                "DELETE FROM foodtime.user_follows WHERE follower_id = ? AND following_id = ?",
                follower, following);
    }

    public long followerCount(UUID userId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM foodtime.user_follows WHERE following_id = ?", Long.class, userId);
    }

    public long followingCount(UUID userId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM foodtime.user_follows WHERE follower_id = ?", Long.class, userId);
    }

    public List<FollowUserResponse> followers(UUID userId, UUID viewer, int limit, long offset) {
        return jdbc.query("""
                SELECT u.id, u.display_name, u.avatar_url,
                       EXISTS(SELECT 1 FROM foodtime.user_follows m
                              WHERE m.follower_id = ? AND m.following_id = u.id) AS followed_by_me
                FROM foodtime.user_follows f JOIN foodtime.users u ON u.id = f.follower_id
                WHERE f.following_id = ? AND u.status = 'active'
                ORDER BY f.created_at DESC, u.id
                LIMIT ? OFFSET ?
                """, ROW, viewer, userId, limit, offset);
    }

    public List<FollowUserResponse> following(UUID userId, UUID viewer, int limit, long offset) {
        return jdbc.query("""
                SELECT u.id, u.display_name, u.avatar_url,
                       EXISTS(SELECT 1 FROM foodtime.user_follows m
                              WHERE m.follower_id = ? AND m.following_id = u.id) AS followed_by_me
                FROM foodtime.user_follows f JOIN foodtime.users u ON u.id = f.following_id
                WHERE f.follower_id = ? AND u.status = 'active'
                ORDER BY f.created_at DESC, u.id
                LIMIT ? OFFSET ?
                """, ROW, viewer, userId, limit, offset);
    }

    public long followerCountVisible(UUID userId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.user_follows f JOIN foodtime.users u ON u.id = f.follower_id
                WHERE f.following_id = ? AND u.status = 'active'
                """, Long.class, userId);
    }

    public long followingCountVisible(UUID userId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.user_follows f JOIN foodtime.users u ON u.id = f.following_id
                WHERE f.follower_id = ? AND u.status = 'active'
                """, Long.class, userId);
    }
}
