package com.FoodtimeNeo.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

abstract class VenueTestSupport extends IntegrationApiSupport {
  @Autowired PlatformTransactionManager transactions;

  String venue(String type) {
    return "/api/v1/" + (type.equals("hall") ? "dining-halls/" + hall : "stalls/" + stall);
  }

  JsonNode submit(Browser user, String type, int rating, String text) throws Exception {
    return submit(user, type, rating, text, UUID.randomUUID());
  }

  JsonNode submit(Browser user, String type, Object rating, String text, UUID request)
      throws Exception {
    var response =
        user.call(
            "POST",
            venue(type) + "/reviews",
            Map.of("rating", rating, "content", text, "clientRequestId", request));
    assertThat(response.statusCode()).as(response.body()).isEqualTo(202);
    return body(response).path("data");
  }

  JsonNode approve(Browser admin, String type, JsonNode submitted) throws Exception {
    var response =
        admin.call(
            "POST",
            "/api/v1/admin/moderation/"
                + type
                + "-review/"
                + submitted.path("submissionId").asString(),
            Map.of("action", "approve"));
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    return body(response).path("data");
  }

  JsonNode data(Browser user, String path) throws Exception {
    var response = user.call("GET", path, null);
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    return body(response).path("data");
  }

  @AfterEach
  void removeVenueFixtures() {
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            tx -> {
              String roots =
                  "SELECT r.id FROM foodtime.venue_reviews r JOIN foodtime.venue_review_targets t"
                      + " ON t.id=r.target_id WHERE t.dining_hall_id='"
                      + hall
                      + "' OR t.stall_id='"
                      + stall
                      + "'";
              String
                  submissions =
                      "SELECT id FROM foodtime.venue_review_submissions WHERE review_id IN("
                          + roots
                          + ")",
                  replies =
                      "SELECT id FROM foodtime.venue_review_replies WHERE review_id IN("
                          + roots
                          + ")";
              jdbc.update(
                  "DELETE FROM foodtime.content_moderation_logs WHERE venue_review_submission_id"
                      + " IN("
                      + submissions
                      + ") OR venue_review_reply_id IN("
                      + replies
                      + ")");
              jdbc.update(
                  "DELETE FROM foodtime.ai_moderation_results WHERE submission_id IN("
                      + submissions
                      + ") OR submission_id IN("
                      + replies
                      + ")");
              jdbc.update(
                  "DELETE FROM foodtime.venue_review_reply_likes WHERE reply_id IN("
                      + replies
                      + ")");
              jdbc.update(
                  "DELETE FROM foodtime.venue_review_likes WHERE review_id IN(" + roots + ")");
              jdbc.update(
                  "DELETE FROM foodtime.venue_review_reply_stats WHERE reply_id IN("
                      + replies
                      + ")");
              jdbc.update(
                  "DELETE FROM foodtime.venue_review_replies WHERE review_id IN(" + roots + ")");
              jdbc.update(
                  "UPDATE foodtime.venue_reviews SET deleted_at=now() WHERE id IN("
                      + roots
                      + ") AND deleted_at IS NULL");
              jdbc.update(
                  "DELETE FROM foodtime.venue_review_stats WHERE review_id IN(" + roots + ")");
              jdbc.update(
                  "DELETE FROM foodtime.venue_review_submissions WHERE review_id IN("
                      + roots
                      + ")");
              jdbc.update("DELETE FROM foodtime.venue_reviews WHERE id IN(" + roots + ")");
            });
  }
}
