package com.FoodtimeNeo.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VenueReviewIT extends VenueTestSupport {
  @ParameterizedTest
  @ValueSource(strings = {"hall", "stall"})
  void pendingIsOnlyInAuthenticatedOwnersPreviewAndNotPublicCount(String type) throws Exception {
    Browser author = login(owner), stranger = login(other);
    var item = submit(author, type, 4, "private <script>alert(1)</script>");
    assertThat(data(author, venue(type) + "/reviews").path("total").asLong()).isZero();
    assertThat(data(author, venue(type) + "/rating").path("averageScore").isNull()).isTrue();
    var mine = author.call("GET", venue(type) + "/reviews/me?userId=" + other.id(), null);
    assertThat(mine.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    assertThat(body(mine).path("data").path("privateSubmission").path("content").asString())
        .contains("private");
    assertThat(
            data(stranger, venue(type) + "/reviews/me?userId=" + owner.id())
                .path("privateSubmission")
                .isNull())
        .isTrue();
    assertThat(
            stranger
                .call(
                    "GET",
                    "/api/v1/users/me/venue-review-submissions/"
                        + item.path("submissionId").asString()
                        + "/status",
                    null)
                .statusCode())
        .isEqualTo(404);
    assertThat(stranger.call("GET", venue(type) + "/reviews", null).body())
        .doesNotContain("private", "userEmail", owner.email());
  }

  @ParameterizedTest
  @ValueSource(strings = {"hall", "stall"})
  void publishesEditsAndDeletesWithoutDoubleCountingOrChangingDishRating(String type)
      throws Exception {
    Browser author = login(owner), admin = login(account("admin"));
    var first = submit(author, type, 3, "original");
    approve(admin, type, first);
    var edit = submit(author, type, 5, "replacement");
    assertThat(
            data(author, venue(type) + "/reviews").path("items").get(0).path("content").asString())
        .isEqualTo("original");
    assertThat(
            data(author, venue(type) + "/reviews/me")
                .path("privateSubmission")
                .path("content")
                .asString())
        .isEqualTo("replacement");
    approve(admin, type, edit);
    var result = data(author, venue(type) + "/reviews");
    assertThat(result.path("items").get(0).path("id").asString())
        .isEqualTo(first.path("id").asString());
    assertThat(result.path("total").asLong()).isEqualTo(1);
    var rating = data(author, venue(type) + "/rating");
    assertThat(rating.path("averageScore").asDouble()).isEqualTo(5);
    assertThat(rating.path("reviewCount").asLong()).isEqualTo(1);
    assertThat(data(author, venue(type) + "/reviews/me").path("privateSubmission").isNull())
        .isTrue();
    assertThat(data(author, dishPath("/rating")).path("reviewCount").asLong()).isZero();
    var stale = submit(author, type, 1, "late edit");
    assertThat(
            author
                .call(
                    "DELETE", "/api/v1/users/me/venue-reviews/" + first.path("id").asString(), null)
                .statusCode())
        .isEqualTo(200);
    assertThat(
            admin
                .call(
                    "POST",
                    "/api/v1/admin/moderation/"
                        + type
                        + "-review/"
                        + stale.path("submissionId").asString(),
                    Map.of("action", "approve"))
                .statusCode())
        .isEqualTo(409);
    assertThat(data(author, venue(type) + "/rating").path("reviewCount").asLong()).isZero();
    assertThat(data(author, venue(type) + "/rating").path("averageScore").isNull()).isTrue();
    var fresh = submit(author, type, 4, "new identity");
    assertThat(fresh.path("id").asString()).isNotEqualTo(first.path("id").asString());
  }

  @Test
  void replayConflictRolesAndInvalidInputs() throws Exception {
    Browser author = login(owner);
    UUID op = UUID.randomUUID();
    var first = submit(author, "hall", 4, "same", op);
    assertThat(submit(author, "hall", 4, "same", op).path("submissionId").asString())
        .isEqualTo(first.path("submissionId").asString());
    assertThat(
            author
                .call(
                    "POST",
                    venue("hall") + "/reviews",
                    Map.of("rating", 5, "content", "changed", "clientRequestId", op))
                .statusCode())
        .isEqualTo(409);
    assertThat(
            author
                .call(
                    "POST",
                    venue("hall") + "/reviews",
                    Map.of("rating", 4, "content", "another", "clientRequestId", UUID.randomUUID()))
                .statusCode())
        .isEqualTo(409);
    for (Object rating : List.of(0, 6, 2.5))
      assertThat(
              author
                  .call(
                      "POST",
                      venue("stall") + "/reviews",
                      Map.of(
                          "rating",
                          rating,
                          "content",
                          "test",
                          "clientRequestId",
                          UUID.randomUUID()))
                  .statusCode())
          .isEqualTo(400);
    for (String content : List.of(" ", "字".repeat(501)))
      assertThat(
              author
                  .call(
                      "POST",
                      venue("stall") + "/reviews",
                      Map.of("rating", 4, "content", content, "clientRequestId", UUID.randomUUID()))
                  .statusCode())
          .isEqualTo(400);
    assertThat(
            login(account("visitor"))
                .call(
                    "POST",
                    venue("hall") + "/reviews",
                    Map.of("rating", 5, "content", "blocked", "clientRequestId", UUID.randomUUID()))
                .statusCode())
        .isEqualTo(403);
    assertThat(author.call("GET", "/api/v1/admin/moderation?type=hall-review", null).statusCode())
        .isEqualTo(403);
  }

  @Test
  void repliesArePrivateUntilApprovedAndLikesAreIdempotentAndIndependent() throws Exception {
    Browser author = login(owner), stranger = login(other), admin = login(account("superadmin"));
    var root = submit(author, "hall", 4, "parent");
    approve(admin, "hall", root);
    String rootId = root.path("id").asString();
    var response =
        stranger.call(
            "POST",
            "/api/v1/venue-reviews/" + rootId + "/replies",
            Map.of("content", "private reply", "clientRequestId", UUID.randomUUID()));
    assertThat(response.statusCode()).as(response.body()).isEqualTo(202);
    String id = body(response).path("data").path("id").asString();
    assertThat(data(author, "/api/v1/venue-reviews/" + rootId + "/replies").path("total").asLong())
        .isZero();
    assertThat(data(author, "/api/v1/venue-reviews/" + rootId + "/replies/me").path("items").size())
        .isZero();
    assertThat(
            data(stranger, "/api/v1/venue-reviews/" + rootId + "/replies/me").path("items").size())
        .isEqualTo(1);
    assertThat(
            author.call("PUT", "/api/v1/venue-review-replies/" + id + "/like", null).statusCode())
        .isEqualTo(404);
    assertThat(
            admin
                .call(
                    "POST",
                    "/api/v1/admin/moderation/hall-review-reply/" + id,
                    Map.of("action", "approve"))
                .statusCode())
        .as("approve reply")
        .isEqualTo(200);
    for (int i = 0; i < 2; i++)
      assertThat(
              body(author.call("PUT", "/api/v1/venue-review-replies/" + id + "/like", null))
                  .path("data")
                  .path("likeCount")
                  .asLong())
          .isEqualTo(1);
    for (int i = 0; i < 2; i++)
      assertThat(
              body(stranger.call("PUT", "/api/v1/venue-reviews/" + rootId + "/like", null))
                  .path("data")
                  .path("likeCount")
                  .asLong())
          .isEqualTo(1);
    assertThat(
            data(author, venue("hall") + "/reviews")
                .path("items")
                .get(0)
                .path("replyCount")
                .asLong())
        .isEqualTo(1);
    assertThat(data(author, venue("hall") + "/rating").path("averageScore").asDouble())
        .isEqualTo(4);
    assertThat(stranger.call("DELETE", "/api/v1/venue-review-replies/" + id, null).statusCode())
        .isEqualTo(200);
    assertThat(data(author, "/api/v1/venue-reviews/" + rootId + "/replies").path("total").asLong())
        .isZero();
  }

  @Test
  void serverFiltersPublishersAndAuditLogAndRejectsDuplicateDecision() throws Exception {
    Browser author = login(owner), admin = login(account("admin"));
    var a = submit(author, "hall", 4, "full audit body");
    var b = submit(author, "stall", 5, "other type");
    var queue = data(admin, "/api/v1/admin/moderation?type=hall-review&status=pending&size=100");
    assertThat(queue.path("total").asLong()).isEqualTo(1);
    assertThat(queue.path("items").get(0).path("userEmail").asString()).isEqualTo(owner.email());
    approve(admin, "hall", a);
    assertThat(
            admin
                .call(
                    "POST",
                    "/api/v1/admin/moderation/hall-review/" + a.path("submissionId").asString(),
                    Map.of("action", "approve"))
                .statusCode())
        .isEqualTo(409);
    assertThat(
            admin
                .call(
                    "POST",
                    "/api/v1/admin/moderation/stall-review/" + b.path("submissionId").asString(),
                    Map.of("action", "reject", "reason", "请修改"))
                .statusCode())
        .isEqualTo(200);
    assertThat(
            data(author, venue("stall") + "/reviews/me")
                .path("privateSubmission")
                .path("reviewReason")
                .asString())
        .isEqualTo("请修改");
    assertThat(admin.call("GET", "/api/v1/admin/moderation/logs?size=100", null).body())
        .contains("full audit body", owner.email(), "hall-review");
  }

  @Test
  void cursorBindsObjectAndFilterAndConcurrentSubmissionsRemainUnique() throws Exception {
    Browser author = login(owner), admin = login(account("admin"));
    var a = submit(author, "hall", 4, "one");
    approve(admin, "hall", a);
    var b = submit(login(other), "hall", 5, "two");
    approve(admin, "hall", b);
    var page = data(author, venue("hall") + "/reviews?size=1");
    String cursor = page.path("nextCursor").asString();
    assertThat(page.path("hasMore").asBoolean()).isTrue();
    assertThat(
            data(author, venue("hall") + "/reviews?size=1&cursor=" + cursor).path("items").size())
        .isEqualTo(1);
    assertThat(author.call("GET", venue("stall") + "/reviews?cursor=" + cursor, null).statusCode())
        .isEqualTo(400);
    assertThat(
            author
                .call("GET", venue("hall") + "/reviews?rating=5&cursor=" + cursor, null)
                .statusCode())
        .isEqualTo(400);
    Browser x = login(owner), y = login(owner);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var calls =
          pool.invokeAll(
              List.of(
                  () ->
                      x.call(
                              "POST",
                              venue("stall") + "/reviews",
                              Map.of(
                                  "rating",
                                  4,
                                  "content",
                                  "one",
                                  "clientRequestId",
                                  UUID.randomUUID()))
                          .statusCode(),
                  () ->
                      y.call(
                              "POST",
                              venue("stall") + "/reviews",
                              Map.of(
                                  "rating",
                                  5,
                                  "content",
                                  "two",
                                  "clientRequestId",
                                  UUID.randomUUID()))
                          .statusCode()));
      assertThat(
              calls.stream()
                  .map(
                      f -> {
                        try {
                          return f.get();
                        } catch (Exception e) {
                          throw new RuntimeException(e);
                        }
                      })
                  .toList())
          .containsExactlyInAnyOrder(202, 409);
    } finally {
      pool.shutdownNow();
    }
  }
}
