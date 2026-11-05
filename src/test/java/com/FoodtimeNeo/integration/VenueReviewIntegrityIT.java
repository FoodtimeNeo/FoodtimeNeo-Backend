package com.FoodtimeNeo.integration;

import static org.assertj.core.api.Assertions.*;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.venue.VenueModels.Pending;
import com.FoodtimeNeo.venue.VenueReviewService;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VenueReviewIntegrityIT extends VenueTestSupport {
  @Autowired VenueReviewService venues;

  private AiVerdict low(String reason) {
    return new AiVerdict("LOW", .99, List.of(), reason, "local-test-model");
  }

  @Test
  void aiDecisionFailureRollsBackVerdictLogPointerAndBuckets() throws Exception {
    Browser user = login(owner);
    var row = submit(user, "hall", 4, "atomic rollback");
    UUID id = UUID.fromString(row.path("submissionId").asString());
    Pending pending = new Pending("hall-review", id, 4, "atomic rollback");
    assertThatThrownBy(() -> venues.recordVerdict(pending, low(" ")))
        .isInstanceOf(RuntimeException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM foodtime.ai_moderation_results WHERE submission_id=?",
                Long.class,
                id))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM foodtime.content_moderation_logs WHERE"
                    + " venue_review_submission_id=?",
                Long.class,
                id))
        .isZero();
    assertThat(data(user, venue("hall") + "/rating").path("reviewCount").asLong()).isZero();
    assertThat(
            data(user, venue("hall") + "/reviews/me")
                .path("privateSubmission")
                .path("status")
                .asString())
        .isEqualTo("pending");
    assertThat(venues.recordVerdict(pending, low("已检查"))).isEqualTo(1);
    assertThat(data(user, venue("hall") + "/rating").path("reviewCount").asLong()).isEqualTo(1);
  }

  @Test
  void manualAndAiRacePublishOneVersionAndOneLog() throws Exception {
    Browser user = login(owner), admin = login(account("admin"));
    var row = submit(user, "stall", 5, "competing decisions");
    UUID id = UUID.fromString(row.path("submissionId").asString());
    Pending pending = new Pending("stall-review", id, 5, "competing decisions");
    var pool = Executors.newFixedThreadPool(2);
    try {
      var results =
          pool.invokeAll(
              List.<Callable<Integer>>of(
                  () -> venues.recordVerdict(pending, low("已检查")),
                  () ->
                      admin
                          .call(
                              "POST",
                              "/api/v1/admin/moderation/stall-review/" + id,
                              Map.of("action", "approve"))
                          .statusCode()));
      for (var result : results) assertThat(result.get()).isIn(0, 1, 200, 409);
    } finally {
      pool.shutdownNow();
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM foodtime.content_moderation_logs WHERE"
                    + " venue_review_submission_id=?",
                Long.class,
                id))
        .isEqualTo(1);
    assertThat(data(user, venue("stall") + "/rating").path("reviewCount").asLong()).isEqualTo(1);
    var detail = data(admin, "/api/v1/admin/venue-moderation/stall-review/" + id);
    assertThat(detail.path("publicVersion").path("submissionId").asString())
        .isEqualTo(id.toString());
    assertThat(
            user.call("GET", "/api/v1/admin/venue-moderation/stall-review/" + id, null)
                .statusCode())
        .isEqualTo(403);
  }

  @Test
  void immutableSubmissionsAndCrossFloorRepliesAreBlockedByDatabaseAndApi() throws Exception {
    Browser user = login(owner), admin = login(account("admin"));
    var h = submit(user, "hall", 4, "immutable");
    var s = submit(user, "stall", 5, "other floor");
    UUID hid = UUID.fromString(h.path("submissionId").asString());
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE foodtime.venue_review_submissions SET content='tampered' WHERE id=?",
                    hid))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE foodtime.venue_reviews SET current_submission_id=? WHERE id=?",
                    hid,
                    UUID.fromString(h.path("id").asString())))
        .isInstanceOf(DataAccessException.class);
    approve(admin, "hall", h);
    approve(admin, "stall", s);
    var reply =
        user.call(
            "POST",
            "/api/v1/venue-reviews/" + h.path("id").asString() + "/replies",
            Map.of("content", "reply", "clientRequestId", UUID.randomUUID()));
    String rid = body(reply).path("data").path("id").asString();
    assertThat(
            admin
                .call(
                    "POST",
                    "/api/v1/admin/moderation/hall-review-reply/" + rid,
                    Map.of("action", "approve"))
                .statusCode())
        .isEqualTo(200);
    assertThat(
            user.call(
                    "POST",
                    "/api/v1/venue-reviews/" + s.path("id").asString() + "/replies",
                    Map.of(
                        "content",
                        "cross floor",
                        "replyToId",
                        rid,
                        "clientRequestId",
                        UUID.randomUUID()))
                .statusCode())
        .isEqualTo(404);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO"
                        + " foodtime.venue_review_replies(id,review_id,user_id,client_request_id,reply_to_id,content)"
                        + " VALUES(?,?,?,?,?,'cross')",
                    UUID.randomUUID(),
                    UUID.fromString(s.path("id").asString()),
                    owner.id(),
                    UUID.randomUUID(),
                    UUID.fromString(rid)))
        .isInstanceOf(DataAccessException.class);
    assertThat(data(user, "/api/v1/dining-halls").toString()).contains(hall.toString(), "averageScore", "reviewCount");
  }
}
