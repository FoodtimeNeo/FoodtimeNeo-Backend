package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrowsingHistoryIT extends IntegrationApiSupport {
    private static final String BASE = "/api/v1/users/me/browsing-history";

    private Map<String, Object> recordBody(long epoch) {
        return Map.of("epoch", epoch, "visitEventId", UUID.randomUUID());
    }

    private long epoch(Browser browser, String kind) throws Exception {
        var data = body(browser.call("GET", BASE + "/settings", null)).path("data");
        return "dish".equals(kind) ? data.path("dishEpoch").asLong() : data.path("postEpoch").asLong();
    }

    @Test
    void recordsVisibleDishMergesRepeatedVisitAndKeepsFifty() throws Exception {
        var browser = login(owner);
        assertThat(browser.call("GET", BASE + "/settings", null).statusCode()).isEqualTo(200);
        assertThat(browser.call("PUT", BASE + "/dish/" + dish, recordBody(0)).statusCode()).isEqualTo(204);
        assertThat(browser.call("PUT", BASE + "/dish/" + dish, recordBody(0)).statusCode()).isEqualTo(204);

        var list = body(browser.call("GET", BASE + "?kind=dish&page=1&size=50", null)).path("data");
        assertThat(list.path("total").asLong()).isEqualTo(1);
        assertThat(list.path("items").get(0).path("available").asBoolean()).isTrue();
        assertThat(list.path("items").get(0).path("title").asString()).isEqualTo("integration dish");

        List<UUID> created = new ArrayList<>();
        for (int index = 0; index < 51; index++) {
            UUID id = UUID.randomUUID();
            created.add(id);
            jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,price) VALUES (?,?,?,?)",
                    id, stall, "history dish " + index, 10);
            assertThat(browser.call("PUT", BASE + "/dish/" + id, recordBody(0)).statusCode()).isEqualTo(204);
        }
        var capped = body(browser.call("GET", BASE + "?kind=dish&page=1&size=50", null)).path("data");
        assertThat(capped.path("total").asLong()).isEqualTo(50);
        assertThat(capped.path("items").size()).isEqualTo(50);
        for (UUID id : created) jdbc.update("DELETE FROM foodtime.dishes WHERE id = ?", id);
    }

    @Test
    void deletionAndClearInvalidateOldEpochsWithoutTouchingTheOtherKind() throws Exception {
        var browser = login(owner);
        assertThat(browser.call("PUT", BASE + "/dish/" + dish, recordBody(0)).statusCode()).isEqualTo(204);
        assertThat(browser.call("DELETE", BASE + "/dish/" + dish, null).statusCode()).isEqualTo(204);
        assertThat(epoch(browser, "dish")).isEqualTo(1);
        var stale = browser.call("PUT", BASE + "/dish/" + dish, recordBody(0));
        assertThat(stale.statusCode()).isEqualTo(409);
        assertThat(body(stale).path("code").asString()).isEqualTo("HISTORY_EPOCH_STALE");
        assertThat(browser.call("PUT", BASE + "/dish/" + dish, recordBody(1)).statusCode()).isEqualTo(204);

        var author = login(owner);
        var postBody = new HashMap<>(communityPost(author, "历史帖子"));
        assertThat(author.call("POST", "/api/v1/community/posts", postBody).statusCode()).isEqualTo(201);
        String postId = body(author.call("GET", "/api/v1/users/me/community-posts?page=1&size=20", null))
                .path("data").path("items").get(0).path("id").asString();
        // Pending posts are not recordable; approve with the same fixture's normal moderation path.
        jdbc.update("UPDATE foodtime.community_posts SET status='approved', reviewed_at=now(), reviewed_by_ai_model='test' WHERE id=?",
                UUID.fromString(postId));
        assertThat(browser.call("PUT", BASE + "/post/" + postId, recordBody(0)).statusCode()).isEqualTo(204);

        assertThat(browser.call("DELETE", BASE + "?kind=dish", null).statusCode()).isEqualTo(204);
        assertThat(epoch(browser, "dish")).isEqualTo(2);
        assertThat(epoch(browser, "post")).isZero();
        assertThat(body(browser.call("GET", BASE + "?kind=dish", null)).path("data").path("total").asLong()).isZero();
        assertThat(body(browser.call("GET", BASE + "?kind=post", null)).path("data").path("total").asLong()).isEqualTo(1);
        assertThat(browser.call("PUT", BASE + "/dish/" + dish, recordBody(1)).statusCode()).isEqualTo(409);
        assertThat(browser.call("PUT", BASE + "/post/" + postId, recordBody(0)).statusCode()).isEqualTo(204);
    }

    @Test
    void disablingStopsNewRecordsAndReEnablingDoesNotReplayOldEpochs() throws Exception {
        var browser = login(owner);
        var settings = body(browser.call("GET", BASE + "/settings", null)).path("data");
        long revision = settings.path("revision").asLong();
        var disabled = browser.call("PUT", BASE + "/settings",
                Map.of("enabled", false, "expectedRevision", revision));
        assertThat(disabled.statusCode()).isEqualTo(200);
        long dishEpoch = body(disabled).path("data").path("dishEpoch").asLong();
        assertThat(dishEpoch).isEqualTo(1);
        assertThat(browser.call("PUT", BASE + "/dish/" + dish, recordBody(0)).statusCode()).isEqualTo(409);

        var enabled = browser.call("PUT", BASE + "/settings",
                Map.of("enabled", true, "expectedRevision", revision + 1));
        assertThat(enabled.statusCode()).isEqualTo(200);
        assertThat(body(enabled).path("data").path("dishEpoch").asLong()).isEqualTo(1);
        assertThat(browser.call("PUT", BASE + "/dish/" + dish, recordBody(0)).statusCode()).isEqualTo(409);
        assertThat(browser.call("PUT", BASE + "/dish/" + dish, recordBody(1)).statusCode()).isEqualTo(204);
    }

    @Test
    void hiddenTargetBecomesUnavailablePlaceholderAndVisitorCanRecord() throws Exception {
        var browser = login(owner);
        assertThat(browser.call("PUT", BASE + "/dish/" + dish, recordBody(0)).statusCode()).isEqualTo(204);
        jdbc.update("UPDATE foodtime.dishes SET status='inactive' WHERE id=?", dish);
        var item = body(browser.call("GET", BASE + "?kind=dish", null)).path("data").path("items").get(0);
        assertThat(item.path("available").asBoolean()).isFalse();
        assertThat(item.path("title").isNull()).isTrue();
        assertThat(browser.call("DELETE", BASE + "/dish/" + dish, null).statusCode()).isEqualTo(204);

        var visitor = login(account("visitor"));
        assertThat(visitor.call("PUT", BASE + "/dish/" + dish, recordBody(0)).statusCode()).isEqualTo(404);
    }

    @Test
    void invalidKindAndUnknownTargetAreRejected() throws Exception {
        var browser = login(owner);
        assertThat(browser.call("GET", BASE + "?kind=reply", null).statusCode()).isEqualTo(400);
        assertThat(browser.call("PUT", BASE + "/post/" + UUID.randomUUID(), recordBody(0)).statusCode())
                .isEqualTo(404);
        assertThat(browser.call("DELETE", BASE, null).statusCode()).isEqualTo(400);
    }
}
