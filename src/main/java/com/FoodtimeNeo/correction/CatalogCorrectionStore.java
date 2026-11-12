package com.FoodtimeNeo.correction;

import com.FoodtimeNeo.correction.CatalogCorrectionDtos.TargetSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CatalogCorrectionStore {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final JdbcTemplate jdbc;

    public CatalogCorrectionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record CorrectionRow(
            UUID id, UUID submitterId, CorrectionTargetType targetType, UUID targetId,
            String targetSummary, long baseRevision, String originalName, String originalDescription,
            BigDecimal originalPrice, String proposedName, String proposedDescription,
            BigDecimal proposedPrice, List<String> changedFields, String reason, String status,
            String reviewNote, UUID reviewedBy, Instant reviewedAt, Instant createdAt, long revision,
            String requestHash) {}

    public record AdminRow(CorrectionRow correction, String submitterName, String submitterEmail) {}

    private static final RowMapper<CorrectionRow> ROW = (rs, index) -> new CorrectionRow(
            rs.getObject("id", UUID.class),
            rs.getObject("submitter_id", UUID.class),
            CorrectionTargetType.fromWire(rs.getString("target_type")).orElseThrow(),
            rs.getObject("target_id", UUID.class),
            rs.getString("target_summary"),
            rs.getLong("base_revision"),
            rs.getString("original_name"),
            rs.getString("original_description"),
            rs.getBigDecimal("original_price"),
            rs.getString("proposed_name"),
            rs.getString("proposed_description"),
            rs.getBigDecimal("proposed_price"),
            textArray(rs, "changed_fields"),
            rs.getString("reason"),
            rs.getString("status"),
            rs.getString("review_note"),
            rs.getObject("reviewed_by", UUID.class),
            instant(rs, "reviewed_at"),
            instant(rs, "created_at"),
            rs.getLong("revision"),
            rs.getString("request_hash"));

    public Optional<CorrectionRow> find(UUID owner, UUID id) {
        return jdbc.query("SELECT * FROM foodtime.catalog_corrections WHERE id = ? AND submitter_id = ?",
                ROW, id, owner).stream().findFirst();
    }

    public Optional<CorrectionRow> findAny(UUID id) {
        return jdbc.query("SELECT * FROM foodtime.catalog_corrections WHERE id = ?", ROW, id)
                .stream().findFirst();
    }

    public Optional<CorrectionRow> findByOwner(UUID owner, UUID id) {
        return find(owner, id);
    }

    public Optional<CorrectionRow> findForUpdate(UUID id) {
        return jdbc.query("SELECT * FROM foodtime.catalog_corrections WHERE id = ? FOR UPDATE", ROW, id)
                .stream().findFirst();
    }

    public Optional<CorrectionRow> findByIdempotency(UUID submitter, String key) {
        return jdbc.query("""
                SELECT * FROM foodtime.catalog_corrections
                WHERE submitter_id = ? AND idempotency_key = ?
                """, ROW, submitter, key).stream().findFirst();
    }

    public List<CorrectionRow> listByOwner(UUID owner, int size, long offset) {
        return jdbc.query("""
                SELECT * FROM foodtime.catalog_corrections
                WHERE submitter_id = ?
                ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?
                """, ROW, owner, size, offset);
    }

    public long countByOwner(UUID owner) {
        Long value = jdbc.queryForObject(
                "SELECT count(*) FROM foodtime.catalog_corrections WHERE submitter_id = ?", Long.class, owner);
        return value == null ? 0 : value;
    }

    public Optional<AdminRow> findAdmin(UUID id) {
        return jdbc.query("""
                SELECT c.*, u.display_name AS submitter_name, u.email AS submitter_email
                FROM foodtime.catalog_corrections c
                JOIN foodtime.users u ON u.id = c.submitter_id
                WHERE c.id = ?
                """, (rs, index) -> new AdminRow(ROW.mapRow(rs, index),
                rs.getString("submitter_name"), rs.getString("submitter_email")), id).stream().findFirst();
    }

    public List<AdminRow> listAdmin(String status, String targetType, int size, long offset) {
        List<Object> args = new ArrayList<>();
        String where = adminWhere(status, targetType, args);
        args.add(size);
        args.add(offset);
        return jdbc.query("""
                SELECT c.*, u.display_name AS submitter_name, u.email AS submitter_email
                FROM foodtime.catalog_corrections c
                JOIN foodtime.users u ON u.id = c.submitter_id
                %s
                ORDER BY c.created_at DESC, c.id DESC LIMIT ? OFFSET ?
                """.formatted(where), (rs, index) -> new AdminRow(ROW.mapRow(rs, index),
                rs.getString("submitter_name"), rs.getString("submitter_email")), args.toArray());
    }

    public long countAdmin(String status, String targetType) {
        List<Object> args = new ArrayList<>();
        return jdbc.queryForObject("SELECT count(*) FROM foodtime.catalog_corrections c"
                + adminWhere(status, targetType, args), Long.class, args.toArray());
    }

    public List<String> evidence(UUID correctionId) {
        return jdbc.query("""
                SELECT image_url FROM foodtime.catalog_correction_evidence
                WHERE correction_id = ? ORDER BY sort_order, image_url
                """, (rs, index) -> rs.getString(1), correctionId);
    }

    public UUID evidenceCorrection(String imageUrl) {
        return jdbc.query("""
                SELECT correction_id FROM foodtime.catalog_correction_evidence
                WHERE image_url = ? LIMIT 1
                """, (rs, index) -> rs.getObject(1, UUID.class), imageUrl).stream().findFirst().orElse(null);
    }

    public void lockDaily(UUID submitter, LocalDate day) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", row -> {},
                "catalog-corrections:" + submitter + ":" + day);
    }

    public long countOnDay(UUID submitter, LocalDate day) {
        Instant start = day.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        Long value = jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.catalog_corrections
                WHERE submitter_id = ? AND created_at >= ? AND created_at < ?
                """, Long.class, submitter, start, end);
        return value == null ? 0 : value;
    }

    public void insert(CorrectionRow row, String idempotencyKey) {
        jdbc.update(con -> {
            PreparedStatement statement = con.prepareStatement("""
                    INSERT INTO foodtime.catalog_corrections
                    (id, submitter_id, target_type, target_id, target_summary, base_revision,
                     original_name, original_description, original_price,
                     proposed_name, proposed_description, proposed_price, changed_fields,
                     reason, idempotency_key, request_hash)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """);
            statement.setObject(1, row.id());
            statement.setObject(2, row.submitterId());
            statement.setString(3, row.targetType().wireName());
            statement.setObject(4, row.targetId());
            statement.setString(5, row.targetSummary());
            statement.setLong(6, row.baseRevision());
            statement.setString(7, row.originalName());
            statement.setString(8, row.originalDescription());
            statement.setBigDecimal(9, row.originalPrice());
            statement.setString(10, row.proposedName());
            statement.setString(11, row.proposedDescription());
            statement.setBigDecimal(12, row.proposedPrice());
            Array fields = con.createArrayOf("text", row.changedFields().toArray());
            statement.setArray(13, fields);
            statement.setString(14, row.reason());
            statement.setString(15, idempotencyKey);
            statement.setString(16, row.requestHash());
            return statement;
        });
    }

    public void insertEvidence(UUID correctionId, UUID uploader, List<String> urls) {
        for (int index = 0; index < urls.size(); index++) {
            jdbc.update("""
                    INSERT INTO foodtime.catalog_correction_evidence
                    (correction_id, sort_order, image_url, uploader_id)
                    VALUES (?, ?, ?, ?)
                    """, correctionId, index, urls.get(index), uploader);
        }
    }

    public void insertEvent(UUID id, UUID correctionId, UUID actorId, String action,
            String publicNote, String internalNote, String originalName, String originalDescription,
            BigDecimal originalPrice, String proposedName, String proposedDescription,
            BigDecimal proposedPrice) {
        jdbc.update("""
                INSERT INTO foodtime.catalog_correction_events
                (id, correction_id, actor_id, action, public_note, internal_note, before_values, after_values)
                VALUES (?, ?, ?, ?, ?, ?,
                    jsonb_build_object('name', ?, 'description', ?, 'price', ?),
                    jsonb_build_object('name', ?, 'description', ?, 'price', ?))
                """, id, correctionId, actorId, action, publicNote, internalNote,
                originalName, originalDescription, originalPrice,
                proposedName, proposedDescription, proposedPrice);
    }

    public boolean decide(UUID id, long expectedRevision, String status, String reviewNote, UUID reviewer) {
        return jdbc.update("""
                UPDATE foodtime.catalog_corrections
                SET status = ?, review_note = ?, reviewed_by = ?, reviewed_at = now(),
                    revision = revision + 1, updated_at = now()
                WHERE id = ? AND revision = ? AND status = 'pending'
                """, status, reviewNote, reviewer, id, expectedRevision) == 1;
    }

    public TargetSnapshot target(CorrectionTargetType type, UUID id, boolean lock) {
        String sql = switch (type) {
            case DINING_HALL -> """
                    SELECT h.id, h.revision, h.name, h.description, NULL::numeric AS price,
                           h.status AS hall_status, NULL::varchar AS stall_status, NULL::varchar AS dish_status,
                           h.name AS summary
                    FROM foodtime.dining_halls h WHERE h.id = ?
                    """ + (lock ? " FOR UPDATE OF h" : "");
            case STALL -> """
                    SELECT s.id, s.revision, s.name, s.description, NULL::numeric AS price,
                           h.status AS hall_status, s.status AS stall_status, NULL::varchar AS dish_status,
                           concat_ws(' / ', h.name, s.name) AS summary
                    FROM foodtime.stalls s JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
                    WHERE s.id = ?
                    """ + (lock ? " FOR UPDATE OF s" : "");
            case DISH -> """
                    SELECT d.id, d.revision, d.name, d.description, d.price,
                           h.status AS hall_status, s.status AS stall_status, d.status AS dish_status,
                           concat_ws(' / ', h.name, s.name, d.name) AS summary
                    FROM foodtime.dishes d
                    JOIN foodtime.stalls s ON s.id = d.stall_id
                    JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id
                    WHERE d.id = ?
                    """ + (lock ? " FOR UPDATE OF d" : "");
        };
        return jdbc.query(sql, (rs, index) -> new TargetSnapshot(
                type.wireName(), rs.getObject("id", UUID.class), rs.getLong("revision"),
                rs.getString("name"), rs.getString("description"), rs.getBigDecimal("price"),
                rs.getString("summary"),
                "active".equals(rs.getString("hall_status"))
                        && (type == CorrectionTargetType.DINING_HALL
                            || "active".equals(rs.getString("stall_status")))
                        && (type != CorrectionTargetType.DISH
                            || "active".equals(rs.getString("dish_status")))), id)
                .stream().findFirst().orElse(null);
    }

    public boolean apply(CorrectionTargetType type, UUID id, long baseRevision,
            List<String> fields, String name, String description, BigDecimal price) {
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (fields.contains("name")) {
            sets.add("name = ?");
            args.add(name);
        }
        if (fields.contains("description")) {
            sets.add("description = ?");
            args.add(description);
        }
        if (fields.contains("price")) {
            sets.add("price = ?");
            args.add(price);
        }
        if (sets.isEmpty()) return false;
        sets.add("revision = revision + 1");
        sets.add("updated_at = now()");
        args.add(id);
        args.add(baseRevision);
        return jdbc.update("UPDATE " + type.table() + " SET " + String.join(", ", sets)
                + " WHERE id = ? AND revision = ?", args.toArray()) == 1;
    }

    private static String adminWhere(String status, String targetType, List<Object> args) {
        List<String> conditions = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            conditions.add("c.status = ?");
            args.add(status);
        }
        if (targetType != null && !targetType.isBlank()) {
            conditions.add("c.target_type = ?");
            args.add(targetType);
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private static List<String> textArray(ResultSet rs, String column) throws SQLException {
        Array array = rs.getArray(column);
        if (array == null) return List.of();
        return List.of((String[]) array.getArray());
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
