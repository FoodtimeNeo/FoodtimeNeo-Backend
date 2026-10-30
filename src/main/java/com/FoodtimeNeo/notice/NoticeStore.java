package com.FoodtimeNeo.notice;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class NoticeStore {
    private final JdbcTemplate jdbc;
    public NoticeStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    // Compatibility for the existing management API. New management is a separate task.
    private String markdown(String title, String body) { return "# " + title.strip() + "\n\n" + body; }
    private NoticeDtos.NoticeResponse row(ResultSet r, int ignored) throws SQLException {
        String content = r.getString("content"), title = "系统公告", body = content;
        if (content.startsWith("# ")) {
            int end = content.indexOf('\n');
            title = end < 0 ? content.substring(2) : content.substring(2, end);
            body = end < 0 ? "" : content.substring(end).stripLeading();
        }
        return new NoticeDtos.NoticeResponse(r.getObject("notice_id", UUID.class), title, body,
                r.getTimestamp("created_time").toInstant(), content);
    }
    public UUID create(UUID actorId, String title, String body) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.notices(notice_id, content, created_by) VALUES (?, ?, ?)",
                id, markdown(title, body), actorId);
        return id;
    }
    public NoticeDtos.NoticeResponse find(UUID id) {
        var rows = jdbc.query("SELECT notice_id, content, created_time FROM foodtime.notices WHERE notice_id = ?",
                this::row, id);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public List<NoticeDtos.NoticeResponse> page(int size, long offset) {
        return jdbc.query("SELECT notice_id, content, created_time FROM foodtime.notices ORDER BY seq DESC LIMIT ? OFFSET ?",
                this::row, size, offset);
    }
    public long count() { return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.notices", Long.class); }
    /** Deprecated: kept for older installed clients. New clients never poll this endpoint. */
    public List<NoticeDtos.NoticeResponse> feed(long sinceSeq, int limit) {
        return jdbc.query("SELECT notice_id, content, created_time FROM foodtime.notices WHERE seq > ? ORDER BY seq ASC LIMIT ?",
                this::row, sinceSeq, limit);
    }
    public long unreadCount(UUID userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.notices n WHERE NOT EXISTS"
                + " (SELECT 1 FROM foodtime.notice_reads r WHERE r.notice_id = n.notice_id AND r.user_id = ?)",
                Long.class, userId);
    }
    public boolean exists(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM foodtime.notices WHERE notice_id = ?)", Boolean.class, id));
    }
    public void markRead(UUID noticeId, UUID userId) {
        jdbc.update("INSERT INTO foodtime.notice_reads(notice_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                noticeId, userId);
    }
    public int update(UUID id, String title, String body) {
        return jdbc.update("UPDATE foodtime.notices SET content = ? WHERE notice_id = ?", markdown(title, body), id);
    }
    public int delete(UUID id) {
        return jdbc.update("DELETE FROM foodtime.notices WHERE notice_id = ?", id);
    }
    public NoticeDtos.CurrentNoticeResponse current(UUID userId) {
        var rows = jdbc.query("""
                SELECT n.notice_id, n.content, n.created_time, r.user_id IS NOT NULL AS is_read,
                       COALESCE(r.dont_show_again, FALSE) AS dont_show_again
                FROM foodtime.config c JOIN foodtime.notices n ON n.notice_id = c.notice_id
                LEFT JOIN foodtime.notice_reads r ON r.notice_id = n.notice_id AND r.user_id = ?
                WHERE c.id = 1 AND NOT COALESCE(r.dont_show_again, FALSE)
                """, (r, i) -> new NoticeDtos.CurrentNoticeResponse(r.getObject("notice_id", UUID.class),
                r.getString("content"), r.getTimestamp("created_time").toInstant(),
                r.getBoolean("is_read"), r.getBoolean("dont_show_again")), userId);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public boolean confirm(UUID noticeId, UUID userId, boolean dontShowAgain) {
        // A late unchecked confirmation from another client must not clear a saved preference.
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                INSERT INTO foodtime.notice_reads(notice_id, user_id, dont_show_again) VALUES (?, ?, ?)
                ON CONFLICT (notice_id, user_id) DO UPDATE
                SET dont_show_again = foodtime.notice_reads.dont_show_again OR EXCLUDED.dont_show_again
                RETURNING dont_show_again
                """, Boolean.class, noticeId, userId, dontShowAgain));
    }
}
