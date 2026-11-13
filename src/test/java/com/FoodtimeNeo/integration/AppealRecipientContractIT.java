package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock;
import org.springframework.boot.test.context.SpringBootTest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;

/** Recipient projections are seeded; authentication, storage and appeal requests use real HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AppealRecipientContractIT extends IntegrationApiSupport {
    @Autowired private Clock clock;
    private static final String DECISIONS = "/api/v1/users/me/moderation-decisions";

    private List<UUID> received(int count) {
        UUID reviewer = account("superadmin").id();
        var ids = new ArrayList<UUID>();
        Instant delivered = clock.instant().minusSeconds(60);
        for (int i = 0; i < count; i++) {
            UUID report = UUID.randomUUID(), decision = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO foodtime.content_reports
                        (id,reporter_id,target_user_id,target_type,target_id,reason,excerpt,reviewed_at,reviewed_by)
                    VALUES (?, ?, ?, 'post', ?, 'fixture report', '', now(), ?)
                    """, report, owner.id(), other.id(), UUID.randomUUID(), reviewer);
            jdbc.update("""
                    INSERT INTO foodtime.report_decisions(id,report_id,outcome,public_reason,internal_note,reviewer_id)
                    VALUES (?, ?, 'upheld', ?, 'internal-only', ?)
                    """, decision, report, "第" + (i + 1) + "条公开决定", reviewer);
            jdbc.update("""
                    INSERT INTO foodtime.report_decision_recipients
                        (decision_id,user_id,side,delivered_at,appeal_deadline)
                    VALUES (?, ?, 'target', ?, ?)
                    """, decision, other.id(), Timestamp.from(delivered.minusSeconds(i)),
                    Timestamp.from(delivered.minusSeconds(i).plusSeconds(7 * 86400)));
            ids.add(decision);
        }
        return ids;
    }

    private String image(Browser browser, int color) throws Exception {
        var picture = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) picture.setRGB(x, y, color);
        var bytes = new ByteArrayOutputStream(); ImageIO.write(picture, "png", bytes);
        var response = browser.upload(bytes.toByteArray());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return body(response).path("data").path("imageUrl").asString();
    }

    @Test
    void restrictedRecipientCanPagePastTwentyAndAppealThatDecisionWithOwnedEvidence() throws Exception {
        var ids = received(23);
        jdbc.update("UPDATE foodtime.users SET status='disabled' WHERE id=?", other.id());
        var target = login(other);
        var firstResponse = target.call("GET", DECISIONS + "?page=1&size=20", null);
        var secondResponse = target.call("GET", DECISIONS + "?page=2&size=20", null);
        assertThat(firstResponse.statusCode()).isEqualTo(200);
        assertThat(secondResponse.statusCode()).isEqualTo(200);
        var first = body(firstResponse).path("data"); var second = body(secondResponse).path("data");
        assertThat(first.path("total").asLong()).isEqualTo(23);
        assertThat(first.path("items").size()).isEqualTo(20);
        assertThat(second.path("page").asInt()).isEqualTo(2);
        assertThat(second.path("items").size()).isEqualTo(3);
        var seen = new HashSet<String>();
        for (var page : List.of(first, second)) for (int i = 0; i < page.path("items").size(); i++)
            assertThat(seen.add(page.path("items").get(i).path("decisionId").asString())).isTrue();
        assertThat(seen).hasSize(23);
        String decision = second.path("items").get(0).path("decisionId").asString();
        assertThat(decision).isEqualTo(ids.get(20).toString());
        assertThat(secondResponse.body()).doesNotContain("internal-only", owner.id().toString());
        var detail = target.call("GET", DECISIONS + "/" + decision, null);
        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(body(detail).path("data").path("reportId").isNull()).isTrue();
        String evidence = image(target, 0xff0000);
        assertThat(target.image(evidence).statusCode()).isEqualTo(200);
        var submitted = target.call("POST", DECISIONS + "/" + decision + "/appeals", Map.of(
                "content", "第21条决定的申诉", "evidenceUrls", List.of(evidence), "clientRequestId", UUID.randomUUID()));
        assertThat(submitted.statusCode()).as(submitted.body()).isEqualTo(201);
        assertThat(body(submitted).path("data").path("evidenceUrls").get(0).asString()).isEqualTo(evidence);
        assertThat(target.call("GET", "/api/v1/users/me/profile", null).statusCode()).isEqualTo(403);
        assertThat(target.call("GET", "/api/v1/community/posts", null).statusCode()).isEqualTo(403);
        var outsider = account("user"); jdbc.update("UPDATE foodtime.users SET status='disabled' WHERE id=?", outsider.id());
        var otherSession = login(outsider);
        assertThat(otherSession.call("GET", DECISIONS + "/" + decision, null).statusCode()).isEqualTo(404);
        assertThat(body(otherSession.call("GET", DECISIONS, null)).path("data").path("total").asLong()).isZero();
    }

    @Test
    void normalRecipientCanSubmitThreeImagesAndReplayWithoutAnotherAppeal() throws Exception {
        UUID decision = received(1).get(0); var target = login(other);
        var images = List.of(image(target, 0xff0000), image(target, 0x00ff00), image(target, 0x0000ff));
        assertThat(target.call("POST", DECISIONS + "/" + decision + "/appeals", Map.of(
                "content", "😀".repeat(2001), "evidenceUrls", images, "clientRequestId", UUID.randomUUID())).statusCode()).isEqualTo(400);
        var request = Map.of("content", "😀".repeat(2000), "evidenceUrls", images, "clientRequestId", UUID.randomUUID());
        var created = target.call("POST", DECISIONS + "/" + decision + "/appeals", request);
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        var replay = target.call("POST", DECISIONS + "/" + decision + "/appeals", request);
        assertThat(replay.statusCode()).isEqualTo(201);
        assertThat(body(replay).path("data").path("id").asString()).isEqualTo(body(created).path("data").path("id").asString());
        assertThat(body(created).path("data").path("evidenceUrls").size()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.report_appeals WHERE decision_id=?", Long.class, decision)).isEqualTo(1L);
        assertThat(target.call("POST", DECISIONS + "/" + decision + "/appeals", Map.of(
                "content", "再次提交", "evidenceUrls", images, "clientRequestId", UUID.randomUUID())).statusCode()).isEqualTo(409);
    }

    @Test
    void evidenceOwnershipAndExpiredDeadlineStillRejectSubmission() throws Exception {
        UUID decision = received(1).get(0); var reporter = login(owner); var target = login(other);
        String foreign = image(reporter, 0xff0000);
        assertThat(target.call("POST", DECISIONS + "/" + decision + "/appeals", Map.of(
                "content", "不允许引用他人图片", "evidenceUrls", List.of(foreign), "clientRequestId", UUID.randomUUID())).statusCode()).isEqualTo(400);
        assertThat(target.call("POST", DECISIONS + "/" + decision + "/appeals", Map.of(
                "content", "超过三张", "evidenceUrls", List.of(foreign, foreign, foreign, foreign), "clientRequestId", UUID.randomUUID())).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.report_appeals WHERE decision_id=?", Long.class, decision)).isZero();
        jdbc.update("UPDATE foodtime.report_decision_recipients SET delivered_at=?, appeal_deadline=? WHERE decision_id=?", Timestamp.from(clock.instant().minusSeconds(8 * 86400)), Timestamp.from(clock.instant().minusSeconds(60)), decision);
        var expired = target.call("POST", DECISIONS + "/" + decision + "/appeals", Map.of(
                "content", "过期申诉", "evidenceUrls", List.of(), "clientRequestId", UUID.randomUUID()));
        assertThat(expired.statusCode()).isEqualTo(409);
        assertThat(body(expired).path("code").asString()).isEqualTo("APPEAL_DEADLINE_EXPIRED");
    }
}
