package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.ai.service.AiTriageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import javax.imageio.ImageIO;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rating-summary.refresh-enabled=false",
                "app.ai.triage.enabled=true",
                "app.ai.triage.provider=fake",
                "app.ai.triage.initial-delay=PT30M"})
class AiTriageIT extends IntegrationApiSupport {
    private static final Path STORAGE = Path.of(System.getProperty("java.io.tmpdir"), "ai-triage-test-" + UUID.randomUUID());
    @DynamicPropertySource static void directory(DynamicPropertyRegistry properties) {
        properties.add("app.storage.directory", () -> STORAGE.toString());
        // Beats the hermetic false from IntegrationApiSupport and any local .env.
        properties.add("app.ai.triage.enabled", () -> "true");
    }

    @Autowired
    private AiTriageService triage;

    @Test
    void fakeProviderDiagnosesReviewsAndPhotosWithoutTouchingTheirStatus() throws Exception {
        Browser browser = login(owner);
        assertThat(browser.call("POST", dishPath("/reviews"), Map.of("rating", 4, "content", "普通点评")).statusCode()).isEqualTo(202);
        UUID second = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,price) VALUES (?, ?, 'ai dish', 5.00)", second, stall);
        assertThat(browser.call("POST", "/api/v1/dishes/" + second + "/reviews", Map.of("rating", 1, "content", "含违禁词的点评")).statusCode()).isEqualTo(202);
        var upload = browser.upload(png());
        assertThat(upload.statusCode()).isEqualTo(201);
        String imageUrl = body(upload).path("data").path("imageUrl").asString();
        assertThat(browser.call("POST", "/api/v1/dishes/" + dish + "/image-submissions",
                Map.of("imageUrl", imageUrl)).statusCode()).isEqualTo(201);

        // The shared dev database may hold older pending rows; instant triage or the sweep covers them.
        // Wait for our three verdicts (instant path usually beats the sweep here), then prove idempotence.
        for (int i = 0; i < 30; i++) {
            Long got = jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.ai_moderation_results WHERE submission_id IN"
                    + " (SELECT id FROM foodtime.dish_review_submissions WHERE content IN ('普通点评','含违禁词的点评'))"
                    + " OR (submission_type = 'photo' AND submission_id IN"
                    + " (SELECT id FROM foodtime.dish_images WHERE image_url = ?))",
                    Long.class, imageUrl);
            if (got != null && got >= 3) break;
            Thread.sleep(1000);
        }
        assertThat(triage.triageOnce()).isZero();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.ai_moderation_results WHERE submission_id IN"
                + " (SELECT id FROM foodtime.dish_review_submissions WHERE content IN ('普通点评','含违禁词的点评'))",
                Long.class)).isEqualTo(2);
        // Confident LOW stays human; confident HIGH is auto-rejected with an AI log entry.
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.dish_review_submissions WHERE content = '普通点评'",
                String.class)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.dish_review_submissions WHERE content = '含违禁词的点评'",
                String.class)).isEqualTo("rejected");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs WHERE actor = 'ai'"
                + " AND review_submission_id IN (SELECT id FROM foodtime.dish_review_submissions WHERE content = '含违禁词的点评')",
                Long.class)).isEqualTo(1);

        Browser admin = login(account("admin"));
        var items = body(admin.call("GET", "/api/v1/admin/moderation?status=pending&size=100", null))
                .path("data").path("items");
        var low = find(items, "普通点评");
        assertThat(low.path("aiRiskLevel").asString()).isEqualTo("LOW");
        assertThat(low.path("aiConfidence").asDouble()).isEqualTo(0.9);
        assertThat(low.path("aiReason").asString()).isNotBlank();
        assertThat(low.path("userDisplayName").asString()).isEqualTo("integration user");
        // Auto-rejected reviews leave the pending queue; humans only see the undecided ones.
        assertThat(StreamSupport.stream(items.spliterator(), false)
                .noneMatch(item -> item.path("content").asString().equals("含违禁词的点评"))).isTrue();
        var photo = findByImage(items, imageUrl);
        assertThat(photo.path("aiRiskLevel").asString()).isEqualTo("LOW");
        assertThat(photo.path("imageUrl").asString()).isEqualTo(imageUrl);
    }

    @Test
    void lowConfidenceVerdictIsEscalatedToSecondLook() throws Exception {
        String content = "存疑 escalation-" + UUID.randomUUID();
        Browser browser = login(owner);
        assertThat(browser.call("POST", dishPath("/reviews"), Map.of("rating", 3, "content", content)).statusCode()).isEqualTo(202);

        // Instant path or sweep: wait for the verdict, then prove the escalation pass ran.
        BigDecimal escalated = null;
        for (int i = 0; i < 30 && escalated == null; i++) {
            Thread.sleep(1000);
            escalated = quietConfidence(content);
        }

        // The fake double answers 0.5 on first sight and 0.95 on second sight;
        // seeing 0.95 here proves the escalation pass ran and its verdict was adopted.
        assertThat(escalated).isEqualByComparingTo("0.95");
    }

    @Test
    void submittedReviewIsDiagnosedWithinSecondsWithoutSweep() throws Exception {
        String content = "即时分诊-" + UUID.randomUUID();
        Browser browser = login(owner);
        assertThat(browser.call("POST", dishPath("/reviews"), Map.of("rating", 5, "content", content)).statusCode()).isEqualTo(202);

        // Never call triageOnce: the submission event alone must drive diagnosis.
        BigDecimal confidence = null;
        for (int i = 0; i < 30 && confidence == null; i++) {
            Thread.sleep(1000);
            confidence = quietConfidence(content);
        }
        assertThat(confidence).isEqualByComparingTo("0.9");
    }

    private BigDecimal quietConfidence(String content) {
        try {
            return jdbc.queryForObject("SELECT confidence FROM foodtime.ai_moderation_results WHERE submission_id IN"
                    + " (SELECT id FROM foodtime.dish_review_submissions WHERE content = ?)",
                    BigDecimal.class, content);
        } catch (org.springframework.dao.EmptyResultDataAccessException expected) {
            return null;
        }
    }

    @Test
    void confidentLowReviewIsAutoApprovedWithAnAiLog() throws Exception {
        String content = "放心通过-" + UUID.randomUUID();
        Browser browser = login(owner);
        assertThat(browser.call("POST", dishPath("/reviews"), Map.of("rating", 5, "content", content)).statusCode()).isEqualTo(202);

        String status = null;
        for (int i = 0; i < 30 && !"approved".equals(status); i++) {
            Thread.sleep(1000);
            status = jdbc.queryForObject("SELECT status FROM foodtime.dish_review_submissions WHERE content = ?",
                    String.class, content);
        }
        assertThat(status).isEqualTo("approved");
        // Published without human touch, and the AI decision is logged separately for verification.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_reviews WHERE content = ?",
                Long.class, content)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT actor FROM foodtime.content_moderation_logs WHERE review_submission_id IN"
                + " (SELECT id FROM foodtime.dish_review_submissions WHERE content = ?)",
                String.class, content)).isEqualTo("ai");
    }

    @Test
    void approvedReviewKeepsAiVerdictVisibleToAdmins() throws Exception {
        String content = "通过可见-" + UUID.randomUUID();
        Browser browser = login(owner);
        assertThat(browser.call("POST", dishPath("/reviews"), Map.of("rating", 4, "content", content)).statusCode()).isEqualTo(202);
        for (int i = 0; i < 30 && quietConfidence(content) == null; i++) {
            Thread.sleep(1000);
        }

        UUID submission = jdbc.queryForObject(
                "SELECT id FROM foodtime.dish_review_submissions WHERE content = ?", UUID.class, content);
        Browser admin = login(account("admin"));
        assertThat(admin.call("POST", "/api/v1/admin/moderation/review/" + submission,
                Map.of("action", "approve")).statusCode()).isEqualTo(200);

        var items = body(admin.call("GET", "/api/v1/admin/reviews?size=100", null))
                .path("data").path("items");
        var row = find(items, content);
        assertThat(row.path("aiRiskLevel").asString()).isEqualTo("LOW");
        assertThat(row.path("aiConfidence").asDouble()).isEqualTo(0.9);
        assertThat(row.path("aiReason").asString()).isNotBlank();
    }

    private static tools.jackson.databind.JsonNode find(tools.jackson.databind.JsonNode items, String content) {
        for (var item : items) {
            if (item.path("content").asString().equals(content)) return item;
        }
        throw new AssertionError("submission missing from queue");
    }

    private static tools.jackson.databind.JsonNode findByImage(tools.jackson.databind.JsonNode items, String imageUrl) {
        for (var item : items) {
            if (item.path("imageUrl").asString().equals(imageUrl)) return item;
        }
        throw new AssertionError("photo missing from queue");
    }

    private byte[] png() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
