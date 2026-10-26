package com.FoodtimeNeo.notice;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * 公告存储：JdbcTemplate 直写 foodtime.notices / foodtime.notice_reads。
 */
@Repository
public class NoticeStore {
    private final JdbcTemplate jdbc;
    public NoticeStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public UUID create(UUID actorId, String title, String body) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.notices(id, title, body, published_at, created_by) VALUES (?, ?, ?, now(), ?)",
                id, title.strip(), body, actorId);
        return id;
    }

    public NoticeDtos.NoticeResponse find(UUID id) {
        var rows = jdbc.query(
                "SELECT id, title, body, published_at FROM foodtime.notices WHERE id = ?",
                (r, n) -> new NoticeDtos.NoticeResponse(
                        r.getObject("id", UUID.class), r.getString("title"), r.getString("body"),
                        r.getTimestamp("published_at").toInstant()),
                id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<NoticeDtos.NoticeResponse> page(int size, long offset) {
        return jdbc.query(
                "SELECT id, title, body, published_at FROM foodtime.notices ORDER BY seq DESC LIMIT ? OFFSET ?",
                (r, n) -> new NoticeDtos.NoticeResponse(
                        r.getObject("id", UUID.class), r.getString("title"), r.getString("body"),
                        r.getTimestamp("published_at").toInstant()),
                size, offset);
    }

    public long count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.notices", Long.class);
    }

    public List<NoticeDtos.NoticeResponse> feed(long sinceSeq, int limit) {
        return jdbc.query(
                "SELECT id, title, body, published_at FROM foodtime.notices WHERE seq > ? ORDER BY seq ASC LIMIT ?",
                (r, n) -> new NoticeDtos.NoticeResponse(
                        r.getObject("id", UUID.class), r.getString("title"), r.getString("body"),
                        r.getTimestamp("published_at").toInstant()),
                sinceSeq, limit);
    }

    public long unreadCount(UUID userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM foodtime.notices n WHERE NOT EXISTS (SELECT 1 FROM foodtime.notice_reads r WHERE r.notice_id = n.id AND r.user_id = ?)",
                Long.class, userId);
    }

    public boolean exists(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM foodtime.notices WHERE id = ?)", Boolean.class, id));
    }

    public void markRead(UUID noticeId, UUID userId) {
        jdbc.update("INSERT INTO foodtime.notice_reads(notice_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                noticeId, userId);
    }

    public int update(UUID id, String title, String body) {
        return jdbc.update("UPDATE foodtime.notices SET title = ?, body = ? WHERE id = ?",
                title.strip(), body, id);
    }

    public int delete(UUID id) {
        return jdbc.update("DELETE FROM foodtime.notices WHERE id = ?", id);
    }
}
