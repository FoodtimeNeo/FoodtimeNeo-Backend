package com.FoodtimeNeo.notice;

import com.FoodtimeNeo.notification.NotificationCategory;
import com.FoodtimeNeo.notification.NotificationPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Lazily materializes eligible notice events for one account; never for drafts/future/expired notices. */
@Service
public class NoticeEventService {
    private final JdbcTemplate jdbc;
    private final NotificationPublisher notifications;
    private final Clock clock;

    public NoticeEventService(JdbcTemplate jdbc, NotificationPublisher notifications, Clock clock) {
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public void ensureVisible(UUID user) {
        var now = clock.instant();
        List<NoticeRow> eligible = jdbc.query("""
                SELECT notice_id, is_update
                FROM foodtime.notices
                WHERE published AND publication_announced
                  AND published_at <= ? AND display_starts_at <= ?
                  AND (display_ends_at IS NULL OR display_ends_at > ?)
                  AND (enabled OR notice_id = (SELECT notice_id FROM foodtime.config WHERE id = 1))
                ORDER BY seq
                """, (rs, index) -> new NoticeRow(rs.getObject("notice_id", UUID.class),
                rs.getBoolean("is_update")), java.sql.Timestamp.from(now),
                java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        for (NoticeRow row : eligible) {
            UUID eventId = UUID.nameUUIDFromBytes(("notice:" + row.id() + ":" + user)
                    .getBytes(StandardCharsets.UTF_8));
            notifications.publishEventOnly(NotificationCategory.NOTICE, user, eventId,
                    row.update() ? "公告有更新" : "收到一条新公告",
                    "请到公告栏查看详情。", "notice", row.id(), null, null);
        }
    }

    private record NoticeRow(UUID id, boolean update) {}
}
