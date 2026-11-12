package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoticeNotificationIT extends IntegrationApiSupport {
    private static final String PREFS = "/api/v1/users/me/notification-preferences";
    private static final String EVENTS = "/api/v1/users/me/notification-events";

    private UUID notice(String content, boolean published, Instant publishedAt, Instant startsAt, Instant endsAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO foodtime.notices
                    (notice_id, content, created_by, published_at, display_starts_at,
                     display_ends_at, published, publication_announced, enabled)
                VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, TRUE)
                """, id, content, owner.id(), Timestamp.from(publishedAt), Timestamp.from(startsAt),
                endsAt == null ? null : Timestamp.from(endsAt), published);
        return id;
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
