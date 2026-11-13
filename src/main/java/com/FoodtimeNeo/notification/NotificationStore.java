package com.FoodtimeNeo.notification;

import com.FoodtimeNeo.notification.NotificationDtos.Event;
import com.FoodtimeNeo.notification.NotificationDtos.Preference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class NotificationStore {
    private final JdbcTemplate jdbc;

    public NotificationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensurePreferences(UUID user) {
        for (NotificationCategory category : NotificationCategory.values()) {
            jdbc.update("""
                    INSERT INTO foodtime.notification_preferences(user_id, category, enabled)
                    VALUES (?, ?, ?) ON CONFLICT (user_id, category) DO NOTHING
                    """, user, category.wireName(), category.defaultEnabled());
        }
    }

    public List<Preference> preferences(UUID user) {
        ensurePreferences(user);
        List<Preference> result = new ArrayList<>();
        for (NotificationCategory category : NotificationCategory.values()) {
            result.add(jdbc.query("""
                    SELECT category, enabled, revision FROM foodtime.notification_preferences
                    WHERE user_id = ? AND category = ?
                    """, (rs, index) -> new Preference(rs.getString("category"), rs.getBoolean("enabled"),
                    rs.getLong("revision")), user, category.wireName()).stream().findFirst().orElseThrow());
        }
        return List.copyOf(result);
    }

    public void lockPreference(UUID user, NotificationCategory category) {
        ensurePreferences(user);
        jdbc.query("SELECT 1 FROM foodtime.notification_preferences WHERE user_id = ? AND category = ? FOR UPDATE",
                row -> {}, user, category.wireName());
    }

    public boolean updatePreference(UUID user, NotificationCategory category, boolean enabled, long expectedRevision) {
        return jdbc.update("""
                UPDATE foodtime.notification_preferences
                SET enabled = ?, revision = revision + 1,
                    enabled_after_seq = CASE WHEN enabled <> ? OR revision = 1 THEN (
                        SELECT COALESCE(max(seq), 0) FROM foodtime.notification_events WHERE recipient_id = ?
                    ) ELSE enabled_after_seq END,
                    updated_at = now()
                WHERE user_id = ? AND category = ? AND revision = ?
                """, enabled, enabled, user, user, category.wireName(), expectedRevision) == 1;
    }

    public Preference preference(UUID user, NotificationCategory category) {
        ensurePreferences(user);
        return jdbc.query("""
                SELECT category, enabled, revision FROM foodtime.notification_preferences
                WHERE user_id = ? AND category = ?
                """, (rs, index) -> new Preference(rs.getString("category"), rs.getBoolean("enabled"),
                rs.getLong("revision")), user, category.wireName()).stream().findFirst().orElseThrow();
    }

    /** Returns the stable sequence for a newly inserted or already existing event. */
    public long insertEvent(UUID eventId, UUID recipient, NotificationCategory category, String title, String body,
            String refType, UUID refId, UUID refCommentId, UUID refRootCommentId) {
        List<Long> inserted = jdbc.query("""
                INSERT INTO foodtime.notification_events
                    (id, recipient_id, category, title, body, ref_type, ref_id, ref_comment_id, ref_root_comment_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING RETURNING seq
                """, (rs, index) -> rs.getLong(1), eventId, recipient, category.wireName(), title, body,
                refType, refId, refCommentId, refRootCommentId);
        if (!inserted.isEmpty()) return inserted.get(0);
        Long existing = jdbc.queryForObject("""
                SELECT seq FROM foodtime.notification_events WHERE id = ? AND recipient_id = ?
                """, Long.class, eventId, recipient);
        if (existing == null) throw new IllegalStateException("notification event belongs to another recipient");
        return existing;
    }

    public void insertInboxMessage(UUID eventId, UUID recipient, NotificationCategory category, String title,
            String body, String refType, UUID refId, UUID refCommentId, UUID refRootCommentId) {
        jdbc.update("""
                INSERT INTO foodtime.messages
                    (id, user_id, title, body, category, ref_type, ref_id, ref_comment_id,
                     ref_root_comment_id, actor_count, is_read, event_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, FALSE, ?)
                ON CONFLICT DO NOTHING
                """, UUID.randomUUID(), recipient, title, body, category.name(), refType, refId,
                refCommentId, refRootCommentId, eventId);
    }

    public boolean deliveryEnabled(UUID user, NotificationCategory category, long seq) {
        return jdbc.query("""
                SELECT enabled, enabled_after_seq FROM foodtime.notification_preferences
                WHERE user_id = ? AND category = ?
                """, (rs, index) -> rs.getBoolean(1) && seq > rs.getLong(2), user, category.wireName())
                .stream().findFirst().orElse(false);
    }

    public void insertDelivery(UUID eventId) {
        jdbc.update("""
                INSERT INTO foodtime.notification_deliveries(event_id, channel, status)
                VALUES (?, 'android', 'pending') ON CONFLICT DO NOTHING
                """, eventId);
    }

    public List<Event> events(UUID user, long afterSeq, int limit) {
        return jdbc.query("""
                SELECT e.id, e.seq, e.category, e.title, e.body, e.ref_type, e.ref_id,
                       e.ref_comment_id, e.ref_root_comment_id, e.created_at,
                       CASE WHEN d.status IS NULL OR d.status = 'delivered' THEN d.status
                            WHEN p.enabled AND e.seq > p.enabled_after_seq THEN d.status
                            ELSE 'skipped' END AS delivery_status
                FROM foodtime.notification_events e
                LEFT JOIN foodtime.notification_deliveries d
                    ON d.event_id = e.id AND d.channel = 'android'
                LEFT JOIN foodtime.notification_preferences p
                    ON p.user_id = e.recipient_id AND p.category = e.category
                WHERE e.recipient_id = ? AND e.seq > ?
                ORDER BY e.seq ASC LIMIT ?
                """, (rs, index) -> new Event(rs.getObject("id", UUID.class), rs.getLong("seq"),
                rs.getString("category"), rs.getString("title"), rs.getString("body"),
                rs.getString("ref_type"), rs.getObject("ref_id", UUID.class),
                rs.getObject("ref_comment_id", UUID.class), rs.getObject("ref_root_comment_id", UUID.class),
                instant(rs, "created_at"), rs.getString("delivery_status")), user, afterSeq, limit);
    }

    public boolean markDelivered(UUID eventId, UUID user) {
        return jdbc.update("""
                UPDATE foodtime.notification_deliveries d
                SET status = 'delivered', delivered_at = COALESCE(delivered_at, now()),
                    attempts = d.attempts + 1, updated_at = now()
                WHERE d.event_id = ? AND d.channel = 'android'
                  AND EXISTS(SELECT 1 FROM foodtime.notification_events e
                             WHERE e.id = d.event_id AND e.recipient_id = ?)
                """, eventId, user) == 1;
    }

    private static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
