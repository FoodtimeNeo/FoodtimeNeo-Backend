package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.notification.NotificationCategory;
import com.FoodtimeNeo.notification.NotificationPublisher;
import com.FoodtimeNeo.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LegacyNotificationMigrationIT extends IntegrationApiSupport {
    private static final String PREFS = "/api/v1/users/me/notification-preferences";
    private static final String EVENTS = "/api/v1/users/me/notification-events";
    @Autowired NotificationPublisher publisher;
    @Autowired NotificationService service;
    private UUID message() {
        UUID id = UUID.randomUUID();
        publisher.publish(NotificationCategory.MESSAGE, owner.id(), id, "私信提醒", "仅测试的私信正文",
                "direct-message", UUID.randomUUID(), null, null);
        return id;
    }

    @Test void legacyEnabledSetsBaselineEvenWhenServerDefaultWasAlreadyTrue() throws Exception {
        var browser = login(owner);
        UUID old = message();
        assertThat(body(browser.call("GET", EVENTS, null)).path("data").path("items").get(0)
                .path("deliveryStatus").asString()).isEqualTo("pending");
        var migrated = body(browser.call("POST", PREFS + "/legacy-message", Map.of("enabled", true))).path("data");
        assertThat(migrated.path("enabled").asBoolean()).isTrue();
        assertThat(migrated.path("revision").asLong()).isEqualTo(2);
        UUID fresh = message();
        var rows = body(browser.call("GET", EVENTS, null)).path("data").path("items");
        assertThat(rows.get(0).path("eventId").asString()).isEqualTo(old.toString());
        assertThat(rows.get(0).path("deliveryStatus").asString()).isEqualTo("skipped");
        assertThat(rows.get(1).path("eventId").asString()).isEqualTo(fresh.toString());
        assertThat(rows.get(1).path("deliveryStatus").asString()).isEqualTo("pending");
        assertThat(body(browser.call("GET", "/api/v1/users/me/messages?page=1&size=20", null))
                .path("data").path("total").asLong()).isEqualTo(2);
    }

    @Test void legacyOffIsPreservedAndRepeatedMigrationCannotFlipItBackOn() throws Exception {
        var browser = login(owner);
        message();
        var first = body(browser.call("POST", PREFS + "/legacy-message", Map.of("enabled", false))).path("data");
        var replay = body(browser.call("POST", PREFS + "/legacy-message", Map.of("enabled", true))).path("data");
        assertThat(first.path("enabled").asBoolean()).isFalse();
        assertThat(replay).isEqualTo(first);
        assertThat(body(browser.call("GET", EVENTS, null)).path("data").path("items").get(0)
                .path("deliveryStatus").asString()).isEqualTo("skipped");
    }

    @Test void explicitNewerOffWinsOverOldLocalOn() throws Exception {
        var browser = login(owner);
        assertThat(browser.call("PUT", PREFS + "/message", Map.of("enabled", false, "expectedRevision", 1)).statusCode()).isEqualTo(200);
        var migrated = body(browser.call("POST", PREFS + "/legacy-message", Map.of("enabled", true))).path("data");
        assertThat(migrated.path("enabled").asBoolean()).isFalse();
        assertThat(migrated.path("revision").asLong()).isEqualTo(2);
    }

    @Test void explicitNewerOnWinsOverOldLocalOffAndDoesNotReplayOldPendingEvents() throws Exception {
        var browser = login(owner);
        message();
        assertThat(browser.call("PUT", PREFS + "/message", Map.of("enabled", true, "expectedRevision", 1)).statusCode()).isEqualTo(200);
        var migrated = body(browser.call("POST", PREFS + "/legacy-message", Map.of("enabled", false))).path("data");
        assertThat(migrated.path("enabled").asBoolean()).isTrue();
        assertThat(migrated.path("revision").asLong()).isEqualTo(2);
        assertThat(body(browser.call("GET", EVENTS, null)).path("data").path("items").get(0)
                .path("deliveryStatus").asString()).isEqualTo("skipped");
    }

    @Test void concurrentMigrationHasOneWinnerAndOneSharedAuthoritativeResult() throws Exception {
        var workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var on = workers.submit(() -> { start.await(); return service.adoptLegacyMessage(owner.id(), true); });
            var off = workers.submit(() -> { start.await(); return service.adoptLegacyMessage(owner.id(), false); });
            start.countDown();
            assertThat(on.get()).isEqualTo(off.get());
            assertThat(on.get().revision()).isEqualTo(2);
        } finally { workers.shutdownNow(); }
    }

    @Test void pendingFromBeforeDisableIsSkippedButDeliveredHistoryRemainsTruthful() throws Exception {
        var browser = login(owner);
        UUID pending = message(), delivered = message();
        assertThat(browser.call("POST", EVENTS + "/" + delivered + "/delivered", null).statusCode()).isEqualTo(204);
        browser.call("PUT", PREFS + "/message", Map.of("enabled", false, "expectedRevision", 1));
        browser.call("PUT", PREFS + "/message", Map.of("enabled", true, "expectedRevision", 2));
        var rows = body(browser.call("GET", EVENTS, null)).path("data").path("items");
        assertThat(rows.get(0).path("eventId").asString()).isEqualTo(pending.toString());
        assertThat(rows.get(0).path("deliveryStatus").asString()).isEqualTo("skipped");
        assertThat(rows.get(1).path("deliveryStatus").asString()).isEqualTo("delivered");
    }

    @Test void missingChoiceIsRejectedAndMigrationOnlyTouchesCurrentAccountMessagePreference() throws Exception {
        var browser = login(owner);
        assertThat(browser.call("POST", PREFS + "/legacy-message", Map.of()).statusCode()).isEqualTo(400);
        browser.call("POST", PREFS + "/legacy-message", Map.of("enabled", false));
        var otherBrowser = login(other);
        var rows = body(otherBrowser.call("GET", PREFS, null)).path("data").path("items");
        assertThat(rows.get(0).path("enabled").asBoolean()).isTrue();
        assertThat(rows.get(0).path("revision").asLong()).isEqualTo(1);
        var own = body(browser.call("GET", PREFS, null)).path("data").path("items");
        for (var item : own) if (!"message".equals(item.path("category").asString())) {
            assertThat(item.path("enabled").asBoolean()).isFalse();
            assertThat(item.path("revision").asLong()).isEqualTo(1);
        }
    }
}
