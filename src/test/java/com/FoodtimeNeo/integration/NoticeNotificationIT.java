package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoticeNotificationIT extends IntegrationApiSupport {
    private static final String PREFS = "/api/v1/users/me/notification-preferences";
    private static final String EVENTS = "/api/v1/users/me/notification-events";
    private final List<UUID> created = new ArrayList<>();

    @AfterEach
    void cleanNotices() {
        for (UUID id : created) jdbc.update("DELETE FROM foodtime.notices WHERE notice_id = ?", id);
    }

    private UUID notice(String content, boolean published, Instant publishedAt, Instant startsAt, Instant endsAt) {
        UUID id = UUID.randomUUID();
        created.add(id);
        jdbc.update("""
                INSERT INTO foodtime.notices
                    (notice_id, content, created_by, published_at, display_starts_at,
                     display_ends_at, published, publication_announced, enabled)
                VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, ?)
                """, id, content, owner.id(), Timestamp.from(publishedAt), Timestamp.from(startsAt),
                endsAt == null ? null : Timestamp.from(endsAt), published, published);
        return id;
    }

    @Test
    void pendingNoticeThatExpiresBeforePollingIsNotRemindedButStaysInArchive() throws Exception {
        var browser = login(owner);
        assertThat(browser.call("PUT", PREFS + "/notice", Map.of("enabled", true, "expectedRevision", 1)).statusCode()).isEqualTo(200);
        Instant now = Instant.now();
        UUID id = notice("# 有截止时间的公告", true, now.minusSeconds(3600), now.minusSeconds(3600), now.plusSeconds(60));
        var before = body(browser.call("GET", EVENTS, null)).path("data");
        assertThat(before.path("items").get(0).path("deliveryStatus").asString()).isEqualTo("pending");
        jdbc.update("UPDATE foodtime.notices SET display_ends_at = ? WHERE notice_id = ?", Timestamp.from(now.minusSeconds(1)), id);
        var expired = body(browser.call("GET", EVENTS, null)).path("data");
        assertThat(expired.path("items").get(0).path("deliveryStatus").asString()).isEqualTo("skipped");
        assertThat(expired.path("nextSeq").asLong()).isEqualTo(before.path("nextSeq").asLong());
        assertThat(body(browser.call("GET", "/api/v1/users/me/notices?page=1&size=20", null))
                .path("data").toString()).contains(id.toString());
    }

    @Test
    void onlyEligibleNoticesCreateEventsAndEnableBaselineDoesNotReplay() throws Exception {
        Instant now = Instant.now();
        UUID active = notice("# 当前公告\n\n正文", true, now, now, null);
        UUID future = notice("# 未来公告\n\n正文", true, now.plusSeconds(86400),
                now.plusSeconds(86400), null);
        UUID expired = notice("# 过期公告\n\n正文", true, now.minusSeconds(172800),
                now.minusSeconds(172800), now.minusSeconds(86400));
        notice("# 草稿公告\n\n正文", false, now, now, null);

        var browser = login(owner);
        var first = body(browser.call("GET", EVENTS + "?afterSeq=0&limit=50", null)).path("data");
        assertThat(first.path("items").size()).isEqualTo(1);
        assertThat(first.path("items").get(0).path("category").asString()).isEqualTo("notice");
        assertThat(first.path("items").get(0).path("refId").asString()).isEqualTo(active.toString());
        assertThat(first.path("items").get(0).path("deliveryStatus").isNull()).isTrue();
        assertThat(first.toString()).doesNotContain(future.toString(), expired.toString());

        var prefs = body(browser.call("GET", PREFS, null)).path("data").path("items");
        long revision = 0;
        for (var item : prefs) if ("notice".equals(item.path("category").asString()))
            revision = item.path("revision").asLong();
        assertThat(browser.call("PUT", PREFS + "/notice",
                Map.of("enabled", true, "expectedRevision", revision)).statusCode()).isEqualTo(200);

        notice("# 新公告\n\n正文", true, Instant.now(), Instant.now(), null);
        var after = body(browser.call("GET", EVENTS + "?afterSeq=" + first.path("nextSeq").asLong()
                + "&limit=50", null)).path("data");
        assertThat(after.path("items").size()).isEqualTo(1);
        assertThat(after.path("items").get(0).path("deliveryStatus").asString()).isEqualTo("pending");
        assertThat(body(browser.call("GET", EVENTS + "?afterSeq=0&limit=50", null))
                .path("data").path("items").get(0).path("deliveryStatus").isNull()).isTrue();
    }
}
