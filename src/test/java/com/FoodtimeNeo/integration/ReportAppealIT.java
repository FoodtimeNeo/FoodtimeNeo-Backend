package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock;
import java.sql.Timestamp;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportAppealIT extends IntegrationApiSupport {
    @Autowired private Clock clock;
    private static final String REPORTS = "/api/v1/users/me/reports";
    private static final String APPEALS = "/api/v1/users/me/appeals";

    private String approvedPost(Browser author) throws Exception {
        var payload = new HashMap<>(communityPost(author, "申诉目标帖子"));
        assertThat(author.call("POST", "/api/v1/community/posts", payload).statusCode()).isEqualTo(202);
        String postId = body(author.call("GET", "/api/v1/users/me/community-posts?page=1&size=20", null))
                .path("data").path("items").get(0).path("id").asString();
        jdbc.update("""
                UPDATE foodtime.community_posts
                SET status='approved', reviewed_at=now(), reviewed_by_ai_model='test'
                WHERE id=?
                """, UUID.fromString(postId));
        return postId;
    }

    private UUID report(Browser reporter, UUID target) throws Exception {
        assertThat(reporter.call("POST", "/api/v1/reports", Map.of(
                "targetType", "post", "targetId", target.toString(), "reason", "需要复核的处理"))
                .statusCode()).isEqualTo(200);
        return jdbc.queryForObject("SELECT id FROM foodtime.content_reports WHERE reporter_id=? AND target_id=?",
                UUID.class, owner.id(), target);
    }

    private String decide(Browser superadmin, UUID reportId) throws Exception {
        var response = superadmin.call("POST", "/api/v1/admin/reports/" + reportId + "/decision",
                Map.of("outcome", "upheld", "publicReason", "初次处理理由"));
        assertThat(response.statusCode()).isEqualTo(200);
        return body(response).path("data").path("id").asString();
    }

    private Map<String, Object> followUp(String content) {
        return Map.of("content", content, "evidenceUrls", java.util.List.of(),
                "clientRequestId", UUID.randomUUID());
    }

    @Test
    void followUpThreadSupportsReplayLimitStaffReplyAndInternalNotes() throws Exception {
        var target = login(other);
        String postId = approvedPost(target);
        var reporter = login(owner);
        UUID reportId = report(reporter, UUID.fromString(postId));

        String key = UUID.randomUUID().toString();
        Map<String, Object> first = Map.of("content", "请补充核对菜单", "evidenceUrls", java.util.List.of(),
                "clientRequestId", key);
        var created = reporter.call("POST", REPORTS + "/" + reportId + "/follow-ups", first);
        assertThat(created.statusCode()).isEqualTo(201);
        String followUpId = body(created).path("data").path("id").asString();
        var replay = reporter.call("POST", REPORTS + "/" + reportId + "/follow-ups", first);
        assertThat(replay.statusCode()).isEqualTo(201);
        assertThat(body(replay).path("data").path("id").asString()).isEqualTo(followUpId);
        assertThat(reporter.call("POST", REPORTS + "/" + reportId + "/follow-ups", Map.of(
                "content", "不同内容", "evidenceUrls", java.util.List.of(), "clientRequestId", key)).statusCode())
                .isEqualTo(409);

        for (int index = 0; index < 4; index++) {
            assertThat(reporter.call("POST", REPORTS + "/" + reportId + "/follow-ups",
                    followUp("第" + index + "次追问")).statusCode()).isEqualTo(201);
        }
        assertThat(reporter.call("POST", REPORTS + "/" + reportId + "/follow-ups",
                followUp("超过额度")).statusCode()).isEqualTo(429);

        var superadmin = login(account("superadmin"));
        var reply = superadmin.call("POST", "/api/v1/admin/reports/" + reportId + "/follow-ups", Map.of(
                "content", "已收到，正在核对", "internalNote", "仅内部备注",
                "evidenceUrls", java.util.List.of(), "clientRequestId", UUID.randomUUID().toString()));
        assertThat(reply.statusCode()).isEqualTo(201);

        var userThread = body(reporter.call("GET", REPORTS + "/" + reportId + "/follow-ups", null)).path("data");
        assertThat(userThread.path("items").size()).isEqualTo(6);
        assertThat(userThread.path("items").get(5).path("content").asString()).isEqualTo("已收到，正在核对");
        assertThat(userThread.path("items").get(5).path("internalNote").isNull()).isTrue();
        var adminThread = body(superadmin.call("GET", "/api/v1/admin/reports/" + reportId + "/follow-ups", null))
                .path("data");
        assertThat(adminThread.path("items").get(5).path("internalNote").asString()).isEqualTo("仅内部备注");
        assertThat(target.call("GET", REPORTS + "/" + reportId + "/follow-ups", null).statusCode()).isEqualTo(404);
        assertThat(body(reporter.call("GET", "/api/v1/users/me/messages", null)).path("data").path("total").asLong())
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void bothSidesCanAppealOnceAndAdminResolutionNotifiesBoth() throws Exception {
        var target = login(other);
        String postId = approvedPost(target);
        var reporter = login(owner);
        UUID reportId = report(reporter, UUID.fromString(postId));
        var superadmin = login(account("superadmin"));
        String decisionId = decide(superadmin, reportId);

        var reporterAppeal = reporter.call("POST", "/api/v1/users/me/moderation-decisions/" + decisionId + "/appeals",
                Map.of("content", "举报人认为处理不充分", "evidenceUrls", java.util.List.of(),
                        "clientRequestId", UUID.randomUUID().toString()));
        assertThat(reporterAppeal.statusCode()).isEqualTo(201);
        assertThat(body(reporterAppeal).path("data").path("side").asString()).isEqualTo("reporter");
        String appealId = body(reporterAppeal).path("data").path("id").asString();
        assertThat(reporter.call("POST", "/api/v1/users/me/moderation-decisions/" + decisionId + "/appeals",
                Map.of("content", "重复申诉", "evidenceUrls", java.util.List.of(),
                        "clientRequestId", UUID.randomUUID().toString())).statusCode()).isEqualTo(409);

        var targetAppeal = target.call("POST", "/api/v1/users/me/moderation-decisions/" + decisionId + "/appeals",
                Map.of("content", "被举报人认为处理有误", "evidenceUrls", java.util.List.of(),
                        "clientRequestId", UUID.randomUUID().toString()));
        assertThat(targetAppeal.statusCode()).isEqualTo(201);
        assertThat(body(targetAppeal).path("data").path("side").asString()).isEqualTo("target");
        assertThat(target.call("GET", APPEALS + "/" + appealId, null).statusCode()).isEqualTo(404);
        assertThat(body(reporter.call("GET", APPEALS, null)).path("data").path("items").size()).isEqualTo(1);

        var admin = login(account("admin"));
        assertThat(admin.call("GET", "/api/v1/admin/appeals", null).statusCode()).isEqualTo(403);
        var queue = body(superadmin.call("GET", "/api/v1/admin/appeals?status=submitted", null)).path("data");
        assertThat(queue.path("items").size()).isEqualTo(2);
        long revision = body(reporterAppeal).path("data").path("revision").asLong();
        String firstId = appealId;
        var started = superadmin.call("POST", "/api/v1/admin/appeals/" + firstId + "/start?expectedRevision=" + revision, null);
        assertThat(started.statusCode()).isEqualTo(200);
        long nextRevision = body(started).path("data").path("revision").asLong();
        var resolved = superadmin.call("POST", "/api/v1/admin/appeals/" + firstId + "/resolve",
                Map.of("outcome", "upheld", "publicReason", "复核后维持原处理",
                        "internalNote", "内部复核记录", "expectedRevision", nextRevision));
        assertThat(resolved.statusCode()).isEqualTo(200);
        assertThat(body(resolved).path("data").path("status").asString()).isEqualTo("resolved");
        var duplicate = superadmin.call("POST", "/api/v1/admin/appeals/" + firstId + "/resolve",
                Map.of("outcome", "rejected", "publicReason", "重复", "expectedRevision", nextRevision + 1));
        assertThat(duplicate.statusCode()).isEqualTo(409);

        var reporterDecisions = body(reporter.call("GET", "/api/v1/users/me/moderation-decisions", null))
                .path("data").path("items");
        assertThat(reporterDecisions.get(0).path("appealStatus").asString()).isEqualTo("resolved");
        assertThat(body(reporter.call("GET", "/api/v1/users/me/messages", null)).path("data").path("total").asLong())
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void appealDeadlineIsServerEnforced() throws Exception {
        var target = login(other);
        String postId = approvedPost(target);
        var reporter = login(owner);
        UUID reportId = report(reporter, UUID.fromString(postId));
        var superadmin = login(account("superadmin"));
        String decisionId = decide(superadmin, reportId);
        jdbc.update("""
                UPDATE foodtime.report_decision_recipients
                SET delivered_at = ?, appeal_deadline = ?
                WHERE decision_id = ? AND user_id = ?
                """, Timestamp.from(clock.instant().minusSeconds(8 * 86400)),
                Timestamp.from(clock.instant().minusSeconds(60)), UUID.fromString(decisionId), other.id());
        var response = target.call("POST", "/api/v1/users/me/moderation-decisions/" + decisionId + "/appeals",
                Map.of("content", "已过期申诉", "evidenceUrls", java.util.List.of(),
                        "clientRequestId", UUID.randomUUID().toString()));
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(body(response).path("code").asString()).isEqualTo("APPEAL_DEADLINE_EXPIRED");
    }
}
