package com.FoodtimeNeo.safety;

import com.FoodtimeNeo.safety.ReportActionDtos.Response;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public class ReportActionStore {
    private final JdbcTemplate jdbc;

    public ReportActionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record CaseRow(UUID decisionId, UUID reportId, String targetType, UUID targetId,
                          UUID targetUserId) {}

    public CaseRow caseForReport(UUID reportId) {
        return jdbc.query("""
                SELECT d.id AS decision_id, r.id AS report_id, r.target_type, r.target_id, r.target_user_id
                FROM foodtime.content_reports r JOIN foodtime.report_decisions d ON d.report_id = r.id
                WHERE r.id = ?
                """, (rs, index) -> new CaseRow(rs.getObject("decision_id", UUID.class),
                rs.getObject("report_id", UUID.class), rs.getString("target_type"),
                rs.getObject("target_id", UUID.class), rs.getObject("target_user_id", UUID.class)), reportId)
                .stream().findFirst().orElse(null);
    }

    public Response existing(UUID decisionId, String actionType, UUID targetId) {
        return jdbc.query("""
                SELECT id, decision_id, action_type, target_type, target_id, performed_by, note, created_at
                FROM foodtime.report_actions
                WHERE decision_id = ? AND action_type = ? AND target_id = ?
                """, (rs, index) -> new Response(rs.getObject("id", UUID.class),
                rs.getObject("decision_id", UUID.class), rs.getString("action_type"),
                rs.getString("target_type"), rs.getObject("target_id", UUID.class),
                rs.getObject("performed_by", UUID.class), rs.getString("note"),
                instant(rs, "created_at")), decisionId, actionType, targetId)
                .stream().findFirst().orElse(null);
    }

    public void insert(UUID id, UUID decisionId, String actionType, String targetType, UUID targetId,
            UUID performedBy, String note) {
        jdbc.update("""
                INSERT INTO foodtime.report_actions
                    (id, decision_id, action_type, target_type, target_id, performed_by, note)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, decisionId, actionType, targetType, targetId, performedBy, note);
    }

    public java.util.List<Response> list(UUID decisionId) {
        return jdbc.query("""
                SELECT id, decision_id, action_type, target_type, target_id, performed_by, note, created_at
                FROM foodtime.report_actions WHERE decision_id = ? ORDER BY created_at, id
                """, (rs, index) -> new Response(rs.getObject("id", UUID.class),
                rs.getObject("decision_id", UUID.class), rs.getString("action_type"),
                rs.getString("target_type"), rs.getObject("target_id", UUID.class),
                rs.getObject("performed_by", UUID.class), rs.getString("note"),
                instant(rs, "created_at")), decisionId);
    }

    private static java.time.Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
