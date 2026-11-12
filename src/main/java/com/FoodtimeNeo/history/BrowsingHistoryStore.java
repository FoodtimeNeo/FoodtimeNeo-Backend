package com.FoodtimeNeo.history;

import com.FoodtimeNeo.history.BrowsingHistoryDtos.Item;
import com.FoodtimeNeo.history.BrowsingHistoryDtos.SettingsResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class BrowsingHistoryStore {
    private static final RowMapper<Item> ITEM = (rs, index) -> new Item(
            rs.getString("kind"), rs.getObject("target_id", UUID.class),
            instant(rs, "visited_at"), rs.getBoolean("available"),
            rs.getString("title"), rs.getString("subtitle"), rs.getString("image_url"));
    private final JdbcTemplate jdbc;

    public BrowsingHistoryStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SettingsResponse settings(UUID user) {
        ensureSettings(user);
        return jdbc.query("""
                SELECT enabled, revision, dish_epoch, post_epoch
                FROM foodtime.browsing_history_settings WHERE user_id = ?
                """, (rs, index) -> new SettingsResponse(rs.getBoolean("enabled"),
                rs.getLong("revision"), rs.getLong("dish_epoch"), rs.getLong("post_epoch")),
                user).stream().findFirst().orElseThrow();
    }

    public void ensureSettings(UUID user) {
        jdbc.update("""
                INSERT INTO foodtime.browsing_history_settings(user_id)
                VALUES (?) ON CONFLICT (user_id) DO NOTHING
                """, user);
    }

    public void lockSettings(UUID user) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", row -> {},
                "browsing-history-settings:" + user);
    }

    public void lockKind(UUID user, HistoryKind kind) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", row -> {},
                "browsing-history:" + user + ":" + kind.wireName());
    }

    /** Returns false when expectedRevision no longer matches. */
    public boolean updateSettings(UUID user, boolean enabled, long expectedRevision) {
        ensureSettings(user);
        return jdbc.update("""
                UPDATE foodtime.browsing_history_settings
                SET enabled = ?, revision = revision + 1,
                    dish_epoch = dish_epoch + CASE WHEN ? AND enabled THEN 1 ELSE 0 END,
                    post_epoch = post_epoch + CASE WHEN ? AND enabled THEN 1 ELSE 0 END,
                    updated_at = now()
                WHERE user_id = ? AND revision = ?
                """, enabled, !enabled, !enabled, user, expectedRevision) == 1;
    }

    public boolean dishVisible(UUID dish) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(
                    SELECT 1 FROM foodtime.dishes d
                    JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
                    JOIN foodtime.dining_halls h ON h.id = s.dining_hall_id AND h.status = 'active'
                    WHERE d.id = ? AND d.status = 'active')
                """, Boolean.class, dish));
    }

    public boolean postVisible(UUID post) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM foodtime.community_posts
                              WHERE id = ? AND status = 'approved' AND deleted_at IS NULL)
                """, Boolean.class, post));
    }

    public void record(UUID user, HistoryKind kind, UUID target, long epoch, UUID eventId) {
        jdbc.update("""
                INSERT INTO foodtime.browsing_history
                    (user_id, kind, target_id, epoch, last_event_id)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (user_id, kind, target_id) DO UPDATE
                SET visited_at = now(), visit_seq = nextval('foodtime.browsing_history_seq'),
                    epoch = EXCLUDED.epoch, last_event_id = EXCLUDED.last_event_id
                WHERE foodtime.browsing_history.last_event_id <> EXCLUDED.last_event_id
                """, user, kind.wireName(), target, epoch, eventId);
        jdbc.update("""
                DELETE FROM foodtime.browsing_history h
                WHERE h.user_id = ? AND h.kind = ?
                  AND h.target_id NOT IN (
                      SELECT k.target_id FROM foodtime.browsing_history k
                      WHERE k.user_id = ? AND k.kind = ?
                      ORDER BY k.visited_at DESC, k.visit_seq DESC LIMIT 50)
                """, user, kind.wireName(), user, kind.wireName());
    }

    public boolean deleteOne(UUID user, HistoryKind kind, UUID target) {
        return jdbc.update("DELETE FROM foodtime.browsing_history WHERE user_id = ? AND kind = ? AND target_id = ?",
                user, kind.wireName(), target) == 1;
    }

    public void clear(UUID user, HistoryKind kind) {
        jdbc.update("DELETE FROM foodtime.browsing_history WHERE user_id = ? AND kind = ?",
                user, kind.wireName());
    }

    public void bumpEpoch(UUID user, HistoryKind kind) {
        ensureSettings(user);
        String column = kind == HistoryKind.DISH ? "dish_epoch" : "post_epoch";
        jdbc.update("UPDATE foodtime.browsing_history_settings SET " + column + " = " + column
                + " + 1, revision = revision + 1, updated_at = now() WHERE user_id = ?", user);
    }

    public List<Item> list(UUID user, HistoryKind kind, int size, long offset) {
        String sql = kind == HistoryKind.DISH ? """
                SELECT h.kind, h.target_id, h.visited_at,
                       (d.id IS NOT NULL AND s.id IS NOT NULL AND dh.id IS NOT NULL) AS available,
                       d.name AS title,
                       CASE WHEN d.id IS NOT NULL THEN concat_ws(' · ', dh.name, s.name) END AS subtitle,
                       CASE WHEN d.id IS NOT NULL THEN (
                           SELECT i.image_url FROM foodtime.dish_images i
                           WHERE i.dish_id = d.id AND i.status = 'approved'
                           ORDER BY i.created_at, i.id LIMIT 1) END AS image_url
                FROM foodtime.browsing_history h
                LEFT JOIN foodtime.dishes d ON d.id = h.target_id AND d.status = 'active'
                LEFT JOIN foodtime.stalls s ON s.id = d.stall_id AND s.status = 'active'
                LEFT JOIN foodtime.dining_halls dh ON dh.id = s.dining_hall_id AND dh.status = 'active'
                WHERE h.user_id = ? AND h.kind = ?
                ORDER BY h.visited_at DESC, h.visit_seq DESC LIMIT ? OFFSET ?
                """ : """
                SELECT h.kind, h.target_id, h.visited_at, (p.id IS NOT NULL) AS available,
                       p.title AS title,
                       CASE WHEN p.id IS NOT NULL THEN u.display_name END AS subtitle,
                       CASE WHEN p.id IS NOT NULL THEN (
                           SELECT i.image_url FROM foodtime.community_post_images i
                           WHERE i.post_id = p.id ORDER BY i.sort_order, i.id LIMIT 1) END AS image_url
                FROM foodtime.browsing_history h
                LEFT JOIN foodtime.community_posts p ON p.id = h.target_id
                    AND p.status = 'approved' AND p.deleted_at IS NULL
                LEFT JOIN foodtime.users u ON u.id = p.user_id
                WHERE h.user_id = ? AND h.kind = ?
                ORDER BY h.visited_at DESC, h.visit_seq DESC LIMIT ? OFFSET ?
                """;
        return jdbc.query(sql, ITEM, user, kind.wireName(), size, offset);
    }

    public long count(UUID user, HistoryKind kind) {
        Long value = jdbc.queryForObject("""
                SELECT count(*) FROM foodtime.browsing_history WHERE user_id = ? AND kind = ?
                """, Long.class, user, kind.wireName());
        return value == null ? 0 : value;
    }

    private static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
