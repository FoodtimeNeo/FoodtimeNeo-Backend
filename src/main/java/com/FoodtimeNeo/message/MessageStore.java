package com.FoodtimeNeo.message;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * 个人消息存储：JdbcTemplate 直写 foodtime.messages。
 */
@Repository
public class MessageStore {
    private final JdbcTemplate jdbc;
    public MessageStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<MessageDtos.MessageResponse> page(UUID userId, int size, long offset) {
        return jdbc.query(
                "SELECT id, title, body, category, ref_type, ref_id, is_read, created_at FROM foodtime.messages WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (r, n) -> row(r), userId, size, offset);
    }

    public long count(UUID userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.messages WHERE user_id = ?", Long.class, userId);
    }

    public long unreadCount(UUID userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.messages WHERE user_id = ? AND NOT is_read", Long.class, userId);
    }

    public boolean owned(UUID id, UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM foodtime.messages WHERE id = ? AND user_id = ?)", Boolean.class, id, userId));
    }

    public int markRead(UUID id, UUID userId) {
        return jdbc.update("UPDATE foodtime.messages SET is_read = TRUE WHERE id = ? AND user_id = ? AND NOT is_read",
                id, userId);
    }

    public int markAllRead(UUID userId) {
        return jdbc.update("UPDATE foodtime.messages SET is_read = TRUE WHERE user_id = ? AND NOT is_read", userId);
    }

    private static MessageDtos.MessageResponse row(java.sql.ResultSet r) throws java.sql.SQLException {
        return new MessageDtos.MessageResponse(
                r.getObject("id", UUID.class), r.getString("title"), r.getString("body"),
                r.getString("category"), r.getString("ref_type"), r.getObject("ref_id", UUID.class),
                r.getBoolean("is_read"), r.getTimestamp("created_at").toInstant());
    }
}
