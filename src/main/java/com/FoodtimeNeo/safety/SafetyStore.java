package com.FoodtimeNeo.safety;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public class SafetyStore {
    private final JdbcTemplate jdbc;

    public SafetyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lockPair(UUID first, UUID second) {
        String a = first.toString(), b = second.toString();
        lock(a.compareTo(b) < 0 ? "block:" + a + ":" + b : "block:" + b + ":" + a);
    }

    public boolean blockedBy(UUID actor, UUID target) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM foodtime.user_blocks WHERE blocker_id=? AND blocked_id=?)
                """, Boolean.class, actor, target));
    }

    public boolean blockedBetween(UUID actor, UUID target) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM foodtime.user_blocks
                              WHERE (blocker_id=? AND blocked_id=?) OR (blocker_id=? AND blocked_id=?))
                """, Boolean.class, actor, target, target, actor));
    }

    public void block(UUID actor, UUID target) {
        jdbc.update("""
                INSERT INTO foodtime.user_blocks(blocker_id, blocked_id)
                SELECT ?, id FROM foodtime.users WHERE id=? AND status='active'
                ON CONFLICT DO NOTHING
                """, actor, target);
    }

    public void unblock(UUID actor, UUID target) {
        jdbc.update("DELETE FROM foodtime.user_blocks WHERE blocker_id=? AND blocked_id=?", actor, target);
    }

    public void lockReporter(UUID reporter) {
        lock("report:" + reporter);
    }

    private void lock(String key) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", row -> {}, key);
    }

    public boolean hasReport(UUID reporter, String type, UUID target) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM foodtime.content_reports
                              WHERE reporter_id=? AND target_type=? AND target_id=?)
                """, Boolean.class, reporter, type, target));
    }

    public long recentReports(UUID reporter) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.content_reports
                WHERE reporter_id=? AND created_at >= now() - interval '1 day'
                """, Long.class, reporter);
    }

    public boolean report(UUID reporter, String type, UUID target, String reason) {
        String source = switch (type) {
            case "post" -> """
                    SELECT user_id, id, left(title || E'\n' || content, 2000)
                    FROM foodtime.community_posts
                    WHERE id=? AND user_id<>? AND status='approved' AND deleted_at IS NULL
                    """;
            case "comment" -> """
                    SELECT c.user_id, c.post_id, c.content
                    FROM foodtime.community_comments c JOIN foodtime.community_posts p ON p.id=c.post_id
                    WHERE c.id=? AND c.user_id<>? AND c.status='approved' AND c.deleted_at IS NULL
                      AND p.status='approved' AND p.deleted_at IS NULL
                    """;
            case "message" -> """
                    SELECT m.sender_id, NULL::uuid, m.content
                    FROM foodtime.direct_messages m JOIN foodtime.conversations c ON c.id=m.conversation_id
                    WHERE m.id=? AND m.sender_id<>? AND (c.user_low=? OR c.user_high=?)
                    """;
            default -> throw new IllegalArgumentException("Unknown report target");
        };
        String insert = """
                INSERT INTO foodtime.content_reports(id, reporter_id, target_type, target_id, reason,
                                                     target_user_id, post_id, excerpt)
                SELECT ?, ?, ?, ?, ?, evidence.* FROM (
                """ + source + ") evidence";
        UUID id = UUID.randomUUID();
        return ("message".equals(type)
                ? jdbc.update(insert, id, reporter, type, target, reason, target, reporter, reporter, reporter)
                : jdbc.update(insert, id, reporter, type, target, reason, target, reporter)) == 1;
    }

    public List<SafetyService.ReportRow> pending(int size, long offset) {
        return jdbc.query("""
                SELECT id, target_type, target_id, target_user_id, post_id, reason, excerpt, created_at
                FROM foodtime.content_reports WHERE reviewed_at IS NULL
                ORDER BY created_at, id LIMIT ? OFFSET ?
                """, (row, index) -> new SafetyService.ReportRow(row.getObject("id", UUID.class),
                row.getString("target_type"), row.getObject("target_id", UUID.class),
                row.getObject("target_user_id", UUID.class), row.getObject("post_id", UUID.class),
                row.getString("reason"), row.getString("excerpt"), row.getTimestamp("created_at").toInstant()),
                size, offset);
    }

    public long pendingCount() {
        return jdbc.queryForObject("SELECT count(*) FROM foodtime.content_reports WHERE reviewed_at IS NULL", Long.class);
    }

    public boolean review(UUID id, UUID reviewer) {
        return jdbc.update("""
                UPDATE foodtime.content_reports
                SET reviewed_at=coalesce(reviewed_at, now()), reviewed_by=coalesce(reviewed_by, ?)
                WHERE id=?
                """, reviewer, id) == 1;
    }
}
