package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rating-summary.refresh-enabled=false")
class PublishedReviewIT extends IntegrationApiSupport {
    @Test
    void submissionUsesTheSessionOwnerAndDoesNotPublishBeforeApproval() throws Exception {
        Browser browser = login(owner);
        var response = browser.call("POST", dishPath("/reviews"), Map.of("rating", 4, "content", "tasty", "userId", other.id(), "status", "approved"));
        assertThat(response.statusCode()).isEqualTo(202);
        var submission = body(response).path("data");
        assertThat(submission.path("status").asString()).isEqualTo("pending");
        UUID id = UUID.fromString(submission.path("id").asString());
        assertThat(jdbc.queryForObject("SELECT user_id FROM foodtime.dish_review_submissions WHERE id = ?", UUID.class, id)).isEqualTo(owner.id());
        assertThat(body(browser.call("GET", dishPath("/reviews"), null)).path("data").path("total").asLong()).isZero();
        assertThat(browser.call("POST", dishPath("/reviews"), Map.of("rating", 5, "content", "again")).statusCode()).isEqualTo(409);
    }

    @Test
    void concurrentRequestsCreateExactlyOnePendingSubmission() throws Exception {
        Browser a = login(owner), b = login(owner);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Future<Integer> first = pool.submit(() -> { start.await(); return a.call("POST", dishPath("/reviews"), Map.of("rating", 3, "content", "first")).statusCode(); });
            Future<Integer> second = pool.submit(() -> { start.await(); return b.call("POST", dishPath("/reviews"), Map.of("rating", 5, "content", "second")).statusCode(); });
            start.countDown();
            assertThat(java.util.List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(202, 409);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_review_submissions WHERE dish_id = ? AND user_id = ?", Long.class, dish, owner.id())).isEqualTo(1);
    }

    @Test
    void publishedReviewsHaveContextAndPendingContentIsNotVisible() throws Exception {
        UUID id = formal(owner, 4);
        Browser browser = login(other);
        browser.call("POST", dishPath("/reviews"), Map.of("rating", 1, "content", "pending private"));
        var response = browser.call("GET", dishPath("/reviews"), null);
        assertThat(response.statusCode()).isEqualTo(200);
        var data = body(response).path("data");
        assertThat(data.path("total").asLong()).isEqualTo(1);
        assertThat(data.path("items").get(0).path("id").asString()).isEqualTo(id.toString());
        assertThat(data.path("items").get(0).path("diningHallName").asString()).isEqualTo("integration hall");
        assertThat(response.body()).doesNotContain("pending private", "avatar", "password");
    }

    @Test
    void onlyTheOwnerCanDeleteTheirPublishedReviewAndHiddenHistoryRemainsReadable() throws Exception {
        UUID id = formal(owner, 4);
        Browser stranger = login(other), author = login(owner);
        assertThat(stranger.call("DELETE", "/api/v1/users/me/reviews/" + id, null).statusCode()).isEqualTo(404);
        assertThat(body(stranger.call("GET", "/api/v1/users/me/reviews", null)).path("data").path("total").asLong()).isZero();
        jdbc.update("UPDATE foodtime.dining_halls SET status = 'inactive' WHERE id = ?", hall);
        assertThat(body(author.call("GET", "/api/v1/users/me/reviews", null)).path("data").path("total").asLong()).isEqualTo(1);
        assertThat(author.call("GET", dishPath("/reviews"), null).statusCode()).isEqualTo(404);
        assertThat(author.call("DELETE", "/api/v1/users/me/reviews/" + id, null).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_reviews WHERE id = ?", Long.class, id)).isZero();
    }

    @Test
    void validationAndHiddenParentsPreventSubmissions() throws Exception {
        Browser browser = login(owner);
        for (int rating : new int[]{0, 6}) {
            assertThat(browser.call("POST", dishPath("/reviews"), Map.of("rating", rating, "content", "invalid")).statusCode()).isEqualTo(400);
        }
        assertThat(browser.call("POST", dishPath("/reviews"), Map.of("rating", 4, "content", " ")).statusCode()).isEqualTo(400);
        assertThat(browser.call("GET", dishPath("/reviews?page=0"), null).statusCode()).isEqualTo(400);
        jdbc.update("UPDATE foodtime.stalls SET status = 'inactive' WHERE id = ?", stall);
        assertThat(browser.call("POST", dishPath("/reviews"), Map.of("rating", 4, "content", "hidden")).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_review_submissions WHERE dish_id = ?", Long.class, dish)).isZero();
    }

    @Test
    void ratingUsesTheMaterializedSnapshotRatherThanRecountingCurrentReviews() throws Exception {
        Browser browser = login(owner);
        var empty = body(browser.call("GET", dishPath("/rating"), null)).path("data");
        assertThat(empty.path("averageScore").isNull()).isTrue();
        assertThat(empty.path("reviewCount").asLong()).isZero();
        var menuBefore = body(browser.call("GET", "/api/v1/stalls/" + stall + "/dishes", null)).path("data").get(0);
        assertThat(menuBefore.path("averageScore").isNull()).isTrue();
        formal(owner, 3);
        jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY foodtime.dish_rating_summary");
        formal(other, 5);
        var snapshot = body(browser.call("GET", dishPath("/rating"), null)).path("data");
        assertThat(snapshot.path("averageScore").asDouble()).isEqualTo(3.0);
        assertThat(snapshot.path("reviewCount").asLong()).isEqualTo(1);
        var menu = body(browser.call("GET", "/api/v1/stalls/" + stall + "/dishes", null)).path("data").get(0);
        assertThat(menu.path("averageScore").asDouble()).isEqualTo(3.0);
        assertThat(menu.path("reviewCount").asLong()).isEqualTo(1);
    }
}
