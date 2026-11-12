package com.FoodtimeNeo.safety;

import com.FoodtimeNeo.safety.ReportDecisionDtos.MyDecision;
import com.FoodtimeNeo.safety.ReportDecisionDtos.MyReport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public class ReportDecisionStore {
    private final JdbcTemplate jdbc;

    public ReportDecisionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ReportRow(UUID id, UUID reporterId, UUID targetUserId, String targetType,
                            UUID targetId, UUID postId, String reason, String excerpt,
                            Instant reviewedAt, long revision) {}

    public ReportRow lockReport(UUID id) {
        return jdbc.query("""
                SELECT id, reporter_id, target_user_id, target_type, target_id, post_id,
                       reason, excerpt, reviewed_at, revision
                FROM foodtime.content_reports WHERE id = ? FOR UPDATE
                """, (rs, index) -> new ReportRow(rs.getObject("id", UUID.class),
                rs.getObject("reporter_id", UUID.class), rs.getObject("target_user_id", UUID.class),
                rs.getString("target_type"), rs.getObject("target_id", UUID.class),
                rs.getObject("post_id", UUID.class), rs.getString("reason"), rs.getString("excerpt"),
                instant(rs, "reviewed_at"), rs.getLong("revision")), id)
                .stream().findFirst().orElse(null);
    }

    public void insertDecision(UUID decisionId, UUID reportId, String outcome, String publicReason,
            String internalNote, UUID reviewer) {
        jdbc.update("""
                INSERT INTO foodtime.report_decisions
                    (id, report_id, outcome, public_reason, internal_note, reviewer_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, decisionId, reportId, outcome, publicReason, internalNote, reviewer);
        jdbc.update("""
                UPDATE foodtime.content_reports
                SET reviewed_at = now(), reviewed_by = ?, revision = revision + 1
                WHERE id = ? AND reviewed_at IS NULL
                """, reviewer, reportId);
    }

    public void insertRecipient(UUID decisionId, UUID userId, String side, Instant deliveredAt,
            Instant appealDeadline, UUID eventId) {
        jdbc.update("""
                INSERT INTO foodtime.report_decision_recipients
                    (decision_id, user_id, side, delivered_at, appeal_deadline, notification_event_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, decisionId, userId, side, java.sql.Timestamp.from(deliveredAt),
                java.sql.Timestamp.from(appealDeadline), eventId);
    }

    public java.util.List<MyReport> myReports(UUID reporter, int size, long offset) {
        return jdbc.query("""
                SELECT r.id, r.target_type, r.target_id, r.post_id, r.reason, r.excerpt,
                       r.created_at, d.id AS decision_id, d.outcome, d.public_reason, d.created_at AS decided_at,
                       CASE WHEN d.id IS NOT NULL THEN 'completed'
                            WHEN r.reviewed_at IS NOT NULL THEN 'legacy'
                            ELSE 'pending' END AS status
                FROM foodtime.content_reports r
                LEFT JOIN foodtime.report_decisions d ON d.report_id = r.id
                WHERE r.reporter_id = ?
                ORDER BY r.created_at DESC, r.id DESC LIMIT ? OFFSET ?
                """, (rs, index) -> new MyReport(rs.getObject("id", UUID.class), rs.getString("target_type"),
                rs.getObject("target_id", UUID.class), rs.getObject("post_id", UUID.class),
                rs.getString("reason"), rs.getString("excerpt"), rs.getString("status"),
                instant(rs, "created_at"), rs.getObject("decision_id", UUID.class),
                rs.getString("outcome"), rs.getString("public_reason"), instant(rs, "decided_at")),
                reporter, size, offset);
    }

    public long myReportCount(UUID reporter) {
        Long value = jdbc.queryForObject("SELECT count(*) FROM foodtime.content_reports WHERE reporter_id = ?",
                Long.class, reporter);
        return value == null ? 0 : value;
    }

    public MyReport myReport(UUID reporter, UUID id) {
        return jdbc.query("""
                SELECT r.id, r.target_type, r.target_id, r.post_id, r.reason, r.excerpt,
                       r.created_at, d.id AS decision_id, d.outcome, d.public_reason, d.created_at AS decided_at,
                       CASE WHEN d.id IS NOT NULL THEN 'completed'
                            WHEN r.reviewed_at IS NOT NULL THEN 'legacy'
                            ELSE 'pending' END AS status
                FROM foodtime.content_reports r
                LEFT JOIN foodtime.report_decisions d ON d.report_id = r.id
                WHERE r.id = ? AND r.reporter_id = ?
                """, (rs, index) -> new MyReport(rs.getObject("id", UUID.class), rs.getString("target_type"),
                rs.getObject("target_id", UUID.class), rs.getObject("post_id", UUID.class),
                rs.getString("reason"), rs.getString("excerpt"), rs.getString("status"),
                instant(rs, "created_at"), rs.getObject("decision_id", UUID.class),
                rs.getString("outcome"), rs.getString("public_reason"), instant(rs, "decided_at")),
                id, reporter).stream().findFirst().orElse(null);
    }

    public java.util.List<MyDecision> myDecisions(UUID user, int size, long offset) {
        return jdbc.query("""
                SELECT d.id AS decision_id, d.report_id, rec.side, r.target_type, d.outcome,
                       d.public_reason, rec.delivered_at, rec.appeal_deadline,
                       a.id AS appeal_id, a.status AS appeal_status, a.outcome AS appeal_outcome
                FROM foodtime.report_decision_recipients rec
                JOIN foodtime.report_decisions d ON d.id = rec.decision_id
                JOIN foodtime.content_reports r ON r.id = d.report_id
                LEFT JOIN foodtime.report_appeals a
                    ON a.decision_id = d.id AND a.appellant_id = rec.user_id
                WHERE rec.user_id = ?
                ORDER BY rec.delivered_at DESC, d.id DESC LIMIT ? OFFSET ?
                """, (rs, index) -> new MyDecision(rs.getObject("decision_id", UUID.class),
                rs.getObject("report_id", UUID.class), rs.getString("side"), rs.getString("target_type"),
                rs.getString("outcome"), rs.getString("public_reason"), instant(rs, "delivered_at"),
                instant(rs, "appeal_deadline"), rs.getObject("appeal_id", UUID.class),
                rs.getString("appeal_status"), rs.getString("appeal_outcome")), user, size, offset);
    }

    public long myDecisionCount(UUID user) {
        Long value = jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.report_decision_recipients WHERE user_id = ?
                """, Long.class, user);
        return value == null ? 0 : value;
    }

    public MyDecision myDecision(UUID user, UUID decisionId) {
        return jdbc.query("""
                SELECT d.id AS decision_id, d.report_id, rec.side, r.target_type, d.outcome,
                       d.public_reason, rec.delivered_at, rec.appeal_deadline,
                       a.id AS appeal_id, a.status AS appeal_status, a.outcome AS appeal_outcome
                FROM foodtime.report_decision_recipients rec
                JOIN foodtime.report_decisions d ON d.id = rec.decision_id
                JOIN foodtime.content_reports r ON r.id = d.report_id
                LEFT JOIN foodtime.report_appeals a
                    ON a.decision_id = d.id AND a.appellant_id = rec.user_id
                WHERE rec.user_id = ? AND d.id = ?
                """, (rs, index) -> new MyDecision(rs.getObject("decision_id", UUID.class),
                rs.getObject("report_id", UUID.class), rs.getString("side"), rs.getString("target_type"),
                rs.getString("outcome"), rs.getString("public_reason"), instant(rs, "delivered_at"),
                instant(rs, "appeal_deadline"), rs.getObject("appeal_id", UUID.class),
                rs.getString("appeal_status"), rs.getString("appeal_outcome")), user, decisionId)
                .stream().findFirst().orElse(null);
    }

    private static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
