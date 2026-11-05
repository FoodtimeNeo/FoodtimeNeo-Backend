package com.FoodtimeNeo.message;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 1:1 私信存储：会话按 user_low < user_high 唯一，双方各有一份已读与删除位置。 */
@Repository
public class DirectMessageStore {
    private final JdbcTemplate jdbc;

    public DirectMessageStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ConversationRow(UUID id, UUID userLow, UUID userHigh,
                                  Instant lowReadAt, Instant highReadAt,
                                  Instant lowDeletedAt, Instant highDeletedAt) {
        public boolean includes(UUID userId) {
            return userId.equals(userLow) || userId.equals(userHigh);
        }
        public boolean deletedFor(UUID userId) {
            return (userId.equals(userLow) ? lowDeletedAt : highDeletedAt) != null;
        }
    }

    public Optional<ConversationRow> findPair(UUID low, UUID high) {
        return jdbc.query("""
                SELECT id,user_low,user_high,low_last_read_at,high_last_read_at,
                       low_deleted_at,high_deleted_at
                FROM foodtime.conversations WHERE user_low=? AND user_high=?
                """, (rs, n) -> row(rs), low, high).stream().findFirst();
    }

    public Optional<ConversationRow> find(UUID id) {
        return jdbc.query("""
                SELECT id,user_low,user_high,low_last_read_at,high_last_read_at,
                       low_deleted_at,high_deleted_at
                FROM foodtime.conversations WHERE id=?
                """, (rs, n) -> row(rs), id).stream().findFirst();
    }

    public void insert(UUID id, UUID low, UUID high) {
        jdbc.update("""
                INSERT INTO foodtime.conversations(id,user_low,user_high)
                VALUES (?,?,?) ON CONFLICT (user_low,user_high) DO NOTHING
                """, id, low, high);
    }

    public List<DirectMessageDtos.ConversationResponse> page(UUID userId, int size, long offset) {
        return jdbc.query("""
                SELECT c.id,c.updated_at,
                       u.id AS other_id,u.display_name,u.avatar_url,
                       lm.content,lm.sender_id,lm.created_at,
                       (SELECT count(*) FROM foodtime.direct_messages m
                        WHERE m.conversation_id=c.id AND m.sender_id<>?
                          AND m.created_at > CASE WHEN c.user_low=?
                              THEN c.low_last_read_at ELSE c.high_last_read_at END) AS unread
                FROM foodtime.conversations c
                JOIN foodtime.users u ON u.id = CASE WHEN c.user_low=? THEN c.user_high ELSE c.user_low END
                LEFT JOIN LATERAL (
                    SELECT content,sender_id,created_at FROM foodtime.direct_messages
                    WHERE conversation_id=c.id ORDER BY created_at DESC,id DESC LIMIT 1
                ) lm ON TRUE
                WHERE (c.user_low=? OR c.user_high=?)
                  AND (CASE WHEN c.user_low=? THEN c.low_deleted_at ELSE c.high_deleted_at END) IS NULL
                ORDER BY c.updated_at DESC,c.id DESC
                LIMIT ? OFFSET ?
                """, (rs, n) -> conversation(rs), userId, userId, userId, userId, userId, userId, size, offset);
    }

    public long count(UUID userId) {
        Long value = jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.conversations c
                WHERE (c.user_low=? OR c.user_high=?)
                  AND (CASE WHEN c.user_low=? THEN c.low_deleted_at ELSE c.high_deleted_at END) IS NULL
                """, Long.class, userId, userId, userId);
        return value == null ? 0 : value;
    }

    public Optional<DirectMessageDtos.ConversationResponse> summary(UUID id, UUID userId) {
        return jdbc.query("""
                SELECT c.id,c.updated_at,
                       u.id AS other_id,u.display_name,u.avatar_url,
                       lm.content,lm.sender_id,lm.created_at,
                       (SELECT count(*) FROM foodtime.direct_messages m
                        WHERE m.conversation_id=c.id AND m.sender_id<>?
                          AND m.created_at > CASE WHEN c.user_low=?
                              THEN c.low_last_read_at ELSE c.high_last_read_at END) AS unread
                FROM foodtime.conversations c
                JOIN foodtime.users u ON u.id = CASE WHEN c.user_low=? THEN c.user_high ELSE c.user_low END
                LEFT JOIN LATERAL (
                    SELECT content,sender_id,created_at FROM foodtime.direct_messages
                    WHERE conversation_id=c.id ORDER BY created_at DESC,id DESC LIMIT 1
                ) lm ON TRUE
                WHERE c.id=? AND (c.user_low=? OR c.user_high=?)
                """, (rs, n) -> conversation(rs), userId, userId, userId, id, userId, userId)
                .stream().findFirst();
    }

    public int markRead(UUID id, UUID userId) {
        return jdbc.update("""
                UPDATE foodtime.conversations SET
                    low_last_read_at=CASE WHEN user_low=? THEN now() ELSE low_last_read_at END,
                    high_last_read_at=CASE WHEN user_high=? THEN now() ELSE high_last_read_at END,
                    low_deleted_at=CASE WHEN user_low=? THEN NULL ELSE low_deleted_at END,
                    high_deleted_at=CASE WHEN user_high=? THEN NULL ELSE high_deleted_at END
                WHERE id=? AND (user_low=? OR user_high=?)
                """, userId, userId, userId, userId, id, userId, userId);
    }

    public int hide(UUID id, UUID userId) {
        return jdbc.update("""
                UPDATE foodtime.conversations SET
                    low_deleted_at=CASE WHEN user_low=? THEN now() ELSE low_deleted_at END,
                    high_deleted_at=CASE WHEN user_high=? THEN now() ELSE high_deleted_at END
                WHERE id=? AND (user_low=? OR user_high=?)
                """, userId, userId, id, userId, userId);
    }

    public void revive(UUID id) {
        jdbc.update("""
                UPDATE foodtime.conversations
                SET low_deleted_at=NULL,high_deleted_at=NULL,updated_at=now()
                WHERE id=?
                """, id);
    }

    public void touch(UUID id) {
        jdbc.update("UPDATE foodtime.conversations SET updated_at=now() WHERE id=?", id);
    }

    public List<DirectMessageDtos.MessageResponse> messages(UUID conversationId, Instant beforeAt,
                                                             UUID beforeId, int limit) {
        if (beforeAt == null) {
            return jdbc.query("""
                    SELECT id,conversation_id,sender_id,content,created_at
                    FROM foodtime.direct_messages WHERE conversation_id=?
                    ORDER BY created_at DESC,id DESC LIMIT ?
                    """, (rs, n) -> message(rs), conversationId, limit);
        }
        return jdbc.query("""
                SELECT id,conversation_id,sender_id,content,created_at
                FROM foodtime.direct_messages WHERE conversation_id=?
                  AND (created_at,id) < (?,?)
                ORDER BY created_at DESC,id DESC LIMIT ?
                """, (rs, n) -> message(rs), conversationId, Timestamp.from(beforeAt), beforeId, limit);
    }

    public int insertMessage(UUID id, UUID conversationId, UUID senderId, String content, UUID clientRequestId) {
        return jdbc.update("""
                INSERT INTO foodtime.direct_messages(id,conversation_id,sender_id,content,client_request_id)
                VALUES (?,?,?,?,?) ON CONFLICT (sender_id,client_request_id) DO NOTHING
                """, id, conversationId, senderId, content, clientRequestId);
    }

    public Optional<DirectMessageDtos.MessageResponse> findByClientRequest(UUID senderId, UUID clientRequestId) {
        return jdbc.query("""
                SELECT id,conversation_id,sender_id,content,created_at
                FROM foodtime.direct_messages WHERE sender_id=? AND client_request_id=?
                """, (rs, n) -> message(rs), senderId, clientRequestId).stream().findFirst();
    }

    public long unreadTotal(UUID userId) {
        Long value = jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.direct_messages m
                JOIN foodtime.conversations c ON c.id=m.conversation_id
                WHERE (c.user_low=? OR c.user_high=?) AND m.sender_id<>?
                  AND (CASE WHEN c.user_low=? THEN c.low_deleted_at ELSE c.high_deleted_at END) IS NULL
                  AND m.created_at > CASE WHEN c.user_low=? THEN c.low_last_read_at ELSE c.high_last_read_at END
                """, Long.class, userId, userId, userId, userId, userId);
        return value == null ? 0 : value;
    }

    public boolean activeUser(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM foodtime.users WHERE id=? AND status='active')", Boolean.class, id));
    }

    private static ConversationRow row(ResultSet rs) throws SQLException {
        return new ConversationRow(rs.getObject("id", UUID.class),
                rs.getObject("user_low", UUID.class), rs.getObject("user_high", UUID.class),
                instant(rs, "low_last_read_at"), instant(rs, "high_last_read_at"),
                instant(rs, "low_deleted_at"), instant(rs, "high_deleted_at"));
    }

    private static DirectMessageDtos.ConversationResponse conversation(ResultSet rs) throws SQLException {
        return new DirectMessageDtos.ConversationResponse(
                rs.getObject("id", UUID.class),
                new DirectMessageDtos.Participant(rs.getObject("other_id", UUID.class),
                        rs.getString("display_name"), rs.getString("avatar_url")),
                rs.getString("content"), rs.getObject("sender_id", UUID.class),
                instant(rs, "created_at"), rs.getLong("unread"), instant(rs, "updated_at"));
    }

    private static DirectMessageDtos.MessageResponse message(ResultSet rs) throws SQLException {
        return new DirectMessageDtos.MessageResponse(rs.getObject("id", UUID.class),
                rs.getObject("conversation_id", UUID.class), rs.getObject("sender_id", UUID.class),
                rs.getString("content"), instant(rs, "created_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
