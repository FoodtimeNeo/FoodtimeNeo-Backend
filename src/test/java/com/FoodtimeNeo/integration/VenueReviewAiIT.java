package com.FoodtimeNeo.integration;

import static org.assertj.core.api.Assertions.*;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.service.AiTriageService;
import com.FoodtimeNeo.venue.VenueModels.Pending;
import com.FoodtimeNeo.venue.VenueReviewService;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.ai.triage.provider=fake",
      "app.ai.triage.initial-delay=PT30M",
      "app.ai.triage.interval=PT30M"
    })
class VenueReviewAiIT extends VenueTestSupport {
  @DynamicPropertySource
  static void enabled(DynamicPropertyRegistry r) {
    r.add("app.ai.triage.enabled", () -> "true");
    r.add("app.ai.single.enabled", () -> "false");
  }

  @Autowired VenueReviewService venues;
  @Autowired AiTriageService triage;

  private void await(UUID id, boolean reply, String expected) throws Exception {
    String table = reply ? "venue_review_replies" : "venue_review_submissions";
    for (int i = 0; i < 100; i++) {
      String state =
          jdbc.queryForObject(
              "SELECT status FROM foodtime." + table + " WHERE id=?", String.class, id);
      if (state.equals(expected)
          && jdbc.queryForObject(
                  "SELECT count(*) FROM foodtime.ai_moderation_results WHERE submission_id=?",
                  Long.class,
                  id)
              == 1) return;
      Thread.sleep(80);
    }
    fail("AI did not reach " + expected + " for " + id);
  }

  @ParameterizedTest
  @ValueSource(strings = {"hall", "stall"})
  void lowApprovesRootAndNestedRepliesExactlyOnce(String type) throws Exception {
    Browser author = login(owner), stranger = login(other);
    var root = submit(author, type, 1, "放心，这次口味差，给一分");
    UUID submission = UUID.fromString(root.path("submissionId").asString());
    await(submission, false, "approved");
    assertThat(data(stranger, venue(type) + "/rating").path("averageScore").asDouble())
        .isEqualTo(1);
    assertThat(data(author, venue(type) + "/reviews/me").path("privateSubmission").isNull())
        .isTrue();
    String id = root.path("id").asString();
    var reply =
        stranger.call(
            "POST",
            "/api/v1/venue-reviews/" + id + "/replies",
            Map.of("content", "放心通过这条回复", "clientRequestId", UUID.randomUUID()));
    assertThat(reply.statusCode()).isEqualTo(202);
    UUID rid = UUID.fromString(body(reply).path("data").path("id").asString());
    await(rid, true, "approved");
    var nested =
        author.call(
            "POST",
            "/api/v1/venue-reviews/" + id + "/replies",
            Map.of(
                "content", "放心，这是楼中楼回复", "replyToId", rid, "clientRequestId", UUID.randomUUID()));
    UUID nid = UUID.fromString(body(nested).path("data").path("id").asString());
    await(nid, true, "approved");
    assertThat(data(author, "/api/v1/venue-reviews/" + id + "/replies").path("total").asLong())
        .isEqualTo(2);
    Pending p = new Pending(type + "-review", submission, 1, "ignored");
    assertThat(venues.recordVerdict(p, new AiVerdict("HIGH", 1, List.of(), "late", "fake")))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM foodtime.content_moderation_logs WHERE"
                    + " venue_review_submission_id=?",
                Long.class,
                submission))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT actor FROM foodtime.content_moderation_logs WHERE"
                    + " venue_review_submission_id=?",
                String.class,
                submission))
        .isEqualTo("ai");
  }

  @Test
  void highRejectsAndUncertainNeverPublishes() throws Exception {
    Browser author = login(owner);
    var rejected = submit(author, "hall", 5, "含违禁词");
    UUID rid = UUID.fromString(rejected.path("submissionId").asString());
    await(rid, false, "rejected");
    assertThat(
            data(author, venue("hall") + "/reviews/me")
                .path("privateSubmission")
                .path("reviewReason")
                .asString())
        .isNotBlank();
    assertThat(data(author, venue("hall") + "/reviews").path("total").asLong()).isZero();
    var pending = submit(author, "stall", 4, "拿不准 " + UUID.randomUUID());
    UUID pid = UUID.fromString(pending.path("submissionId").asString());
    await(pid, false, "pending");
    assertThat(data(author, venue("stall") + "/rating").path("reviewCount").asLong()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM foodtime.content_moderation_logs WHERE"
                    + " venue_review_submission_id=?",
                Long.class,
                pid))
        .isZero();
  }
}
