package com.FoodtimeNeo.safety;

import com.FoodtimeNeo.safety.ReportInteractionDtos.Appeal;
import com.FoodtimeNeo.safety.ReportInteractionDtos.FollowUp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ReportInteractionStore {
    private final JdbcTemplate jdbc;

    public ReportInteractionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Recipient(UUID userId, String side, Instant deliveredAt, Instant appealDeadline) {}

    public UUID reporter(UUID reportId) {
        return jdbc.query("SELECT reporter_id FROM foodtime.content_reports WHERE id = ?",
                (rs, index) -> rs.getObject(1, UUID.class), reportId).stream().findFirst().orElse(null);
    }

    public Recipient recipient(UUID decisionId, UUID userId) {
        return jdbc.query("""
                SELECT user_id, side, delivered_at, appeal_deadline
                FROM foodtime.report_decision_recipients
                WHERE decision_id = ? AND user_id = ?
                """, (rs, index) -> new Recipient(rs.getObject("user_id", UUID.class), rs.getString("side"),
                instant(rs, "delivered_at"), instant(rs, "appeal_deadline")), decisionId, userId)
                .stream().findFirst().orElse(null);
    }

    public List<Recipient> recipients(UUID decisionId) {
        return jdbc.query("""
                SELECT user_id, side, delivered_at, appeal_deadline
                FROM foodtime.report_decision_recipients WHERE decision_id = ?
                """, (rs, index) -> new Recipient(rs.getObject("user_id", UUID.class), rs.getString("side"),
                instant(rs, "delivered_at"), instant(rs, "appeal_deadline")), decisionId);
    }

    public FollowUp followUp(UUID id, UUID reportId, boolean includeInternal) {
        return jdbc.query("""
                SELECT f.id, f.report_id, f.author_role, f.content, f.internal_note, f.created_at,
                       u.display_name
                FROM foodtime.report_followups f JOIN foodtime.users u ON u.id = f.author_id
                WHERE f.id = ? AND f.report_id = ?
                """, (rs, index) -> new FollowUp(rs.getObject("id", UUID.class),
                rs.getObject("report_id", UUID.class), rs.getString("author_role"),
                rs.getString("display_name"), rs.getString("content"),
                includeInternal ? rs.getString("internal_note") : null, List.of(),
                instant(rs, "created_at")), id, reportId).stream().findFirst().orElse(null);
    }

    public FollowUp findFollowUpByRequest(UUID author, UUID requestId) {
        return jdbc.query("""
                SELECT f.id, f.report_id, f.author_role, f.content, f.internal_note, f.created_at,
                       u.display_name
                FROM foodtime.report_followups f JOIN foodtime.users u ON u.id = f.author_id
                WHERE f.author_id = ? AND f.client_request_id = ?
                """, (rs, index) -> new FollowUp(rs.getObject("id", UUID.class),
                rs.getObject("report_id", UUID.class), rs.getString("author_role"),
                rs.getString("display_name"), rs.getString("content"),
                rs.getString("internal_note"), evidence("followup", rs.getObject("id", UUID.class)),
                instant(rs, "created_at")), author, requestId).stream().findFirst().orElse(null);
    }

    public FollowUp insertFollowUp(UUID id, UUID reportId, UUID author, String role, String content,
            String internalNote, UUID requestId, List<String> evidence) {
        jdbc.update("""
                INSERT INTO foodtime.report_followups
                    (id, report_id, author_id, author_role, content, internal_note, client_request_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, reportId, author, role, content, internalNote, requestId);
        insertEvidence("followup", id, author, evidence);
        return followUp(id, reportId, true);
    }

    public void insertStaffFollowUp(UUID id, UUID reportId, UUID author, String content,
            String internalNote, UUID requestId, List<String> evidence) {
        jdbc.update("""
                INSERT INTO foodtime.report_followups
                    (id, report_id, author_id, author_role, content, internal_note, client_request_id)
                VALUES (?, ?, ?, 'staff', ?, ?, ?)
                """, id, reportId, author, content, internalNote, requestId);
        insertEvidence("followup", id, author, evidence);
    }

    public List<FollowUp> followUps(UUID reportId, boolean includeInternal, int size, long offset) {
        return jdbc.query("""
                SELECT f.id, f.report_id, f.author_role, f.content, f.internal_note, f.created_at,
                       u.display_name
                FROM foodtime.report_followups f JOIN foodtime.users u ON u.id = f.author_id
                WHERE f.report_id = ?
                ORDER BY f.created_at, f.id LIMIT ? OFFSET ?
                """, (rs, index) -> new FollowUp(rs.getObject("id", UUID.class),
                rs.getObject("report_id", UUID.class), rs.getString("author_role"),
                rs.getString("display_name"), rs.getString("content"),
                includeInternal ? rs.getString("internal_note") : null,
                evidence("followup", rs.getObject("id", UUID.class)), instant(rs, "created_at")),
                reportId, size, offset);
    }

    public long followUpCount(UUID reportId) {
        Long value = jdbc.queryForObject("SELECT count(*) FROM foodtime.report_followups WHERE report_id = ?",
                Long.class, reportId);
        return value == null ? 0 : value;
    }

    public long followUpToday(UUID author, Instant start, Instant end) {
        Long value = jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.report_followups
                WHERE author_id = ? AND author_role = 'reporter' AND created_at >= ? AND created_at < ?
                """, Long.class, author, java.sql.Timestamp.from(start), java.sql.Timestamp.from(end));
        return value == null ? 0 : value;
    }

    public void lockFollowUpDay(UUID author, java.time.LocalDate day) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", row -> {},
                "report-followup:" + author + ":" + day);
    }

    public Appeal findAppealByRequest(UUID appellant, UUID requestId) {
        return jdbc.query("""
                SELECT a.*, u.display_name
                FROM foodtime.report_appeals a JOIN foodtime.users u ON u.id = a.appellant_id
                WHERE a.appellant_id = ? AND a.client_request_id = ?
                """, (rs, index) -> appeal(rs), appellant, requestId).stream().findFirst().orElse(null);
    }

    public Appeal existingAppeal(UUID decisionId, UUID appellant) {
        return jdbc.query("""
                SELECT a.*, u.display_name
                FROM foodtime.report_appeals a JOIN foodtime.users u ON u.id = a.appellant_id
                WHERE a.decision_id = ? AND a.appellant_id = ?
                """, (rs, index) -> appeal(rs), decisionId, appellant).stream().findFirst().orElse(null);
    }

    public Appeal insertAppeal(UUID id, UUID decisionId, UUID appellant, String side, String content,
            UUID requestId, List<String> evidence) {
        jdbc.update("""
                INSERT INTO foodtime.report_appeals
                    (id, decision_id, appellant_id, side, content, client_request_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, decisionId, appellant, side, content, requestId);
        insertEvidence("appeal", id, appellant, evidence);
        return appeal(id);
    }

    public Appeal appeal(UUID id) {
        return jdbc.query("""
                SELECT a.*, u.display_name
                FROM foodtime.report_appeals a JOIN foodtime.users u ON u.id = a.appellant_id
                WHERE a.id = ?
                """, (rs, index) -> appeal(rs), id).stream().findFirst().orElse(null);
    }

    public Appeal appealForUser(UUID id, UUID user) {
        return jdbc.query("""
                SELECT a.*, u.display_name
                FROM foodtime.report_appeals a JOIN foodtime.users u ON u.id = a.appellant_id
                WHERE a.id = ? AND a.appellant_id = ?
                """, (rs, index) -> appeal(rs), id, user).stream().findFirst().orElse(null);
    }

    public List<Appeal> appealsForUser(UUID user, int size, long offset) {
        return jdbc.query("""
                SELECT a.*, u.display_name
                FROM foodtime.report_appeals a JOIN foodtime.users u ON u.id = a.appellant_id
                WHERE a.appellant_id = ?
                ORDER BY a.created_at DESC, a.id DESC LIMIT ? OFFSET ?
                """, (rs, index) -> appeal(rs), user, size, offset);
    }

    public long appealsForUserCount(UUID user) {
        Long value = jdbc.queryForObject("SELECT count(*) FROM foodtime.report_appeals WHERE appellant_id = ?",
                Long.class, user);
        return value == null ? 0 : value;
    }

    public List<Appeal> appeals(String status, int size, long offset) {
        if (status == null) {
            return jdbc.query("""
                    SELECT a.*, u.display_name FROM foodtime.report_appeals a
                    JOIN foodtime.users u ON u.id = a.appellant_id
                    ORDER BY a.created_at DESC, a.id DESC LIMIT ? OFFSET ?
                    """, (rs, index) -> appeal(rs), size, offset);
        }
        return jdbc.query("""
                SELECT a.*, u.display_name FROM foodtime.report_appeals a
                JOIN foodtime.users u ON u.id = a.appellant_id
                WHERE a.status = ? ORDER BY a.created_at DESC, a.id DESC LIMIT ? OFFSET ?
                """, (rs, index) -> appeal(rs), status, size, offset);
    }

    public long appealsCount(String status) {
        Long value = status == null
                ? jdbc.queryForObject("SELECT count(*) FROM foodtime.report_appeals", Long.class)
                : jdbc.queryForObject("SELECT count(*) FROM foodtime.report_appeals WHERE status = ?",
                        Long.class, status);
        return value == null ? 0 : value;
    }

    public boolean startAppeal(UUID id, long expectedRevision) {
        return jdbc.update("""
                UPDATE foodtime.report_appeals
                SET status = 'under_review', revision = revision + 1
                WHERE id = ? AND status = 'submitted' AND revision = ?
                """, id, expectedRevision) == 1;
    }

    public boolean resolveAppeal(UUID id, long expectedRevision, String outcome, String publicReason,
            String internalNote, UUID reviewer) {
        return jdbc.update("""
                UPDATE foodtime.report_appeals
                SET status = 'resolved', outcome = ?, public_reason = ?, internal_note = ?,
                    reviewer_id = ?, resolved_at = now(), revision = revision + 1
                WHERE id = ? AND status IN ('submitted', 'under_review') AND revision = ?
                """, outcome, publicReason, internalNote, reviewer, id, expectedRevision) == 1;
    }

    public List<String> evidence(String kind, UUID refId) {
        return jdbc.query("""
                SELECT image_url FROM foodtime.report_evidence
                WHERE kind = ? AND ref_id = ? ORDER BY sort_order, image_url
                """, (rs, index) -> rs.getString(1), kind, refId);
    }

    public void insertEvidence(String kind, UUID refId, UUID uploader, List<String> urls) {
        for (int index = 0; index < urls.size(); index++) {
            jdbc.update("""
                    INSERT INTO foodtime.report_evidence
                        (id, kind, ref_id, uploader_id, image_url, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), kind, refId, uploader, urls.get(index), index);
        }
    }

    public boolean evidenceImage(String imageUrl) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM foodtime.report_evidence WHERE image_url = ?)
                """, Boolean.class, imageUrl));
    }

    private Appeal appeal(java.sql.ResultSet rs) throws java.sql.SQLException {
        UUID id = rs.getObject("id", UUID.class);
        return new Appeal(id, rs.getObject("decision_id", UUID.class),
                rs.getObject("appellant_id", UUID.class), rs.getString("display_name"),
                rs.getString("side"), rs.getString("content"), rs.getString("status"),
                rs.getString("outcome"), rs.getString("public_reason"), rs.getString("internal_note"),
                rs.getLong("revision"), instant(rs, "created_at"), instant(rs, "resolved_at"),
                evidence("appeal", id));
    }

    private static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
