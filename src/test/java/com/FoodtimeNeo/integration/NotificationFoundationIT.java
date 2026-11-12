package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.notification.NotificationCategory;
import com.FoodtimeNeo.notification.NotificationPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationFoundationIT extends IntegrationApiSupport {
    private static final String PREFS = "/api/v1/users/me/notification-preferences";
    private static final String EVENTS = "/api/v1/users/me/notification-events";

    @Autowired
    private NotificationPublisher notifications;

    private UUID publish(String title, UUID eventId) {
        UUID ref = UUID.randomUUID();
        notifications.publish(NotificationCategory.CORRECTION, owner.id(), eventId, title, "通用提醒正文",
                "catalog-correction", ref, null, null);
        return ref;
    }

    @Test
    void defaultsKeepInboxReadableAndCreateDeliveryOnlyWhenEnabled() throws Exception {
        var browser = login(owner);
        var prefs = body(browser.call("GET", PREFS, null)).path("data").path("items");
        assertThat(prefs.size()).isEqualTo(7);
        assertThat(prefs.get(0).path("category").asString()).isEqualTo("message");
        assertThat(prefs.get(0).path("enabled").asBoolean()).isTrue();
        var correction = find(prefs, "correction");
        assertThat(correction.path("enabled").asBoolean()).isFalse();

        UUID first = UUID.randomUUID();
        UUID ref = publish("第一条纠错通知", first);
        var events = body(browser.call("GET", EVENTS + "?afterSeq=0&limit=50", null)).path("data");
        assertThat(events.path("items").size()).isEqualTo(1);
        assertThat(events.path("items").get(0).path("eventId").asString()).isEqualTo(first.toString());
        assertThat(events.path("items").get(0).path("refId").asString()).isEqualTo(ref.toString());
        assertThat(events.path("items").get(0).path("deliveryStatus").isNull()).isTrue();

        var messages = body(browser.call("GET", "/api/v1/users/me/messages?page=1&size=20", null)).path("data");
        assertThat(messages.path("total").asLong()).isEqualTo(1);
        assertThat(messages.path("items").get(0).path("category").asString()).isEqualTo("CORRECTION");

        long revision = correction.path("revision").asLong();
        var enabled = browser.call("PUT", PREFS + "/correction",
                Map.of("enabled", true, "expectedRevision", revision));
        assertThat(enabled.statusCode()).isEqualTo(200);
        long secondRevision = body(enabled).path("data").path("revision").asLong();

        UUID second = UUID.randomUUID();
        publish("第二条纠错通知", second);
        var afterFirst = body(browser.call("GET", EVENTS + "?afterSeq="
                + events.path("nextSeq").asLong() + "&limit=50", null)).path("data");
        assertThat(afterFirst.path("items").size()).isEqualTo(1);
        assertThat(afterFirst.path("items").get(0).path("eventId").asString()).isEqualTo(second.toString());
        assertThat(afterFirst.path("items").get(0).path("deliveryStatus").asString()).isEqualTo("pending");

        assertThat(browser.call("POST", EVENTS + "/" + second + "/delivered", null).statusCode()).isEqualTo(204);
        var delivered = body(browser.call("GET", EVENTS + "?afterSeq="
                + events.path("nextSeq").asLong() + "&limit=50", null)).path("data");
        assertThat(delivered.path("items").get(0).path("deliveryStatus").asString()).isEqualTo("delivered");

        // Replaying the same eventId is idempotent for the event, inbox projection and delivery.
        publish("第二条纠错通知", second);
        assertThat(body(browser.call("GET", EVENTS + "?afterSeq=0&limit=50", null))
                .path("data").path("items").size()).isEqualTo(2);
        assertThat(body(browser.call("GET", "/api/v1/users/me/messages?page=1&size=20", null))
                .path("data").path("total").asLong()).isEqualTo(2);
        assertThat(secondRevision).isGreaterThan(revision);
    }

    @Test
    void disablingThenReenablingDoesNotDeliverEventsFromTheDisabledWindow() throws Exception {
        var browser = login(owner);
        var correction = find(body(browser.call("GET", PREFS, null)).path("data").path("items"), "correction");
        long revision = correction.path("revision").asLong();
        assertThat(browser.call("PUT", PREFS + "/correction",
                Map.of("enabled", true, "expectedRevision", revision)).statusCode()).isEqualTo(200);

        var current = find(body(browser.call("GET", PREFS, null)).path("data").path("items"), "correction");
        assertThat(browser.call("PUT", PREFS + "/correction",
                Map.of("enabled", false, "expectedRevision", current.path("revision").asLong())).statusCode())
                .isEqualTo(200);
        UUID disabledEvent = UUID.randomUUID();
        publish("关闭期间事件", disabledEvent);
        var disabledRows = body(browser.call("GET", EVENTS + "?afterSeq=0&limit=50", null)).path("data");
        assertThat(disabledRows.path("items").get(0).path("deliveryStatus").isNull()).isTrue();

        var off = find(body(browser.call("GET", PREFS, null)).path("data").path("items"), "correction");
        assertThat(browser.call("PUT", PREFS + "/correction",
                Map.of("enabled", true, "expectedRevision", off.path("revision").asLong())).statusCode())
                .isEqualTo(200);
        var reenabled = body(browser.call("GET", EVENTS + "?afterSeq=0&limit=50", null)).path("data");
        assertThat(reenabled.path("items").get(0).path("deliveryStatus").isNull()).isTrue();

        UUID fresh = UUID.randomUUID();
        publish("重新开启后事件", fresh);
        var freshRow = body(browser.call("GET", EVENTS + "?afterSeq=" + disabledRows.path("nextSeq").asLong()
                + "&limit=50", null)).path("data").path("items").get(0);
        assertThat(freshRow.path("eventId").asString()).isEqualTo(fresh.toString());
        assertThat(freshRow.path("deliveryStatus").asString()).isEqualTo("pending");
    }

    @Test
    void otherUsersCannotReadOrMarkEvents() throws Exception {
        publish("私有通知", UUID.randomUUID());
        var otherBrowser = login(other);
        assertThat(body(otherBrowser.call("GET", EVENTS, null)).path("data").path("items").size()).isZero();
        assertThat(otherBrowser.call("POST", EVENTS + "/" + UUID.randomUUID() + "/delivered", null).statusCode())
                .isEqualTo(409);
    }

    private static tools.jackson.databind.JsonNode find(tools.jackson.databind.JsonNode items, String category) {
        for (var item : items) if (category.equals(item.path("category").asString())) return item;
        throw new AssertionError("missing category " + category);
    }
}
