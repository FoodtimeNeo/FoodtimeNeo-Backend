package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportDecisionIT extends IntegrationApiSupport {
    private static final String REPORTS = "/api/v1/users/me/reports";
    private static final String DECISIONS = "/api/v1/users/me/moderation-decisions";

    private String approvedPost(Browser author) throws Exception {
        var payload = new HashMap<>(communityPost(author, "举报目标帖子"));
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

    private UUID reportId(UUID reporter, UUID target) {
        return jdbc.queryForObject("""
                SELECT id FROM foodtime.content_reports WHERE reporter_id=? AND target_id=?
                """, UUID.class, reporter, target);
    }

    @Test
    void decisionCreatesPrivateReceiptsAndAppealDeadline() throws Exception {
        var targetBrowser = login(other);
        String postId = approvedPost(targetBrowser);
        var reporter = login(owner);
        assertThat(reporter.call("POST", "/api/v1/reports",
                Map.of("targetType", "post", "targetId", postId, "reason", "内容不合适")).statusCode())
                .isEqualTo(200);
        UUID reportId = reportId(owner.id(), UUID.fromString(postId));

        var admin = login(account("admin"));
        assertThat(admin.call("POST", "/api/v1/admin/reports/" + reportId + "/decision",
                Map.of("outcome", "upheld", "publicReason", "公开理由")).statusCode()).isEqualTo(403);

        var superadmin = login(account("superadmin"));
        var decided = superadmin.call("POST", "/api/v1/admin/reports/" + reportId + "/decision",
                Map.of("outcome", "upheld", "publicReason", "经核对，内容违反社区规范",
                        "internalNote", "仅内部可见"));
        assertThat(decided.statusCode()).isEqualTo(200);
        String decisionId = body(decided).path("data").path("id").asString();
        assertThat(body(decided).path("data").path("internalNote").asString()).isEqualTo("仅内部可见");

        var mine = body(reporter.call("GET", REPORTS, null)).path("data").path("items");
        assertThat(mine.size()).isEqualTo(1);
        assertThat(mine.get(0).path("status").asString()).isEqualTo("completed");
        assertThat(mine.get(0).path("outcome").asString()).isEqualTo("upheld");
        assertThat(mine.get(0).path("publicReason").asString()).contains("违反社区规范");

        var reporterDecision = body(reporter.call("GET", DECISIONS, null))
                .path("data").path("items").get(0);
        assertThat(reporterDecision.path("side").asString()).isEqualTo("reporter");
        assertThat(reporterDecision.path("reportId").asString()).isEqualTo(reportId.toString());
        Instant delivered = Instant.parse(reporterDecision.path("deliveredAt").asString());
        Instant deadline = Instant.parse(reporterDecision.path("appealDeadline").asString());
        assertThat(Duration.between(delivered, deadline).toDays()).isEqualTo(7);
        assertThat(reporterDecision.path("reportId").isNull()).isFalse();

        var targetDecision = body(targetBrowser.call("GET", DECISIONS, null))
                .path("data").path("items").get(0);
        assertThat(targetDecision.path("side").asString()).isEqualTo("target");
        assertThat(targetDecision.path("reportId").isNull()).isTrue();
        assertThat(targetDecision.path("outcome").asString()).isEqualTo("upheld");

        var otherUser = login(account("user"));
        assertThat(otherUser.call("GET", DECISIONS + "/" + decisionId, null).statusCode()).isEqualTo(404);
        assertThat(reporter.call("GET", DECISIONS + "/" + decisionId, null).statusCode()).isEqualTo(200);
        assertThat(targetBrowser.call("GET", DECISIONS + "/" + decisionId, null).statusCode()).isEqualTo(200);
        assertThat(reporter.call("GET", DECISIONS + "/" + UUID.randomUUID(), null).statusCode()).isEqualTo(404);

        assertThat(body(reporter.call("GET", "/api/v1/users/me/notification-events", null))
                .path("data").path("items").get(0).path("category").asString()).isEqualTo("report");
        assertThat(body(targetBrowser.call("GET", "/api/v1/users/me/notification-events", null))
                .path("data").path("items").get(0).path("category").asString()).isEqualTo("report");
        assertThat(body(reporter.call("GET", "/api/v1/users/me/messages", null))
                .path("data").path("total").asLong()).isEqualTo(1);

        assertThat(superadmin.call("POST", "/api/v1/admin/reports/" + reportId + "/decision",
                Map.of("outcome", "not_upheld", "publicReason", "重复处理")).statusCode()).isEqualTo(409);
    }

    @Test
    void linkedAdminActionIsPerformedByServerAndVisibleOnlyToRecipients() throws Exception {
        var targetBrowser = login(other);
        String postId = approvedPost(targetBrowser);
        var reporter = login(owner);
        assertThat(reporter.call("POST", "/api/v1/reports",
                Map.of("targetType", "post", "targetId", postId, "reason", "需要下架")).statusCode()).isEqualTo(200);
        UUID reportId = reportId(owner.id(), UUID.fromString(postId));
        var superadmin = login(account("superadmin"));
        String decisionId = body(superadmin.call("POST", "/api/v1/admin/reports/" + reportId + "/decision",
                Map.of("outcome", "upheld", "publicReason", "内容违规"))).path("data").path("id").asString();

        var action = superadmin.call("POST", "/api/v1/admin/reports/" + reportId + "/actions",
                Map.of("actionType", "remove_post", "note", "管理员执行下架"));
        assertThat(action.statusCode()).isEqualTo(201);
        String actionId = body(action).path("data").path("id").asString();
        assertThat(body(action).path("data").path("targetType").asString()).isEqualTo("post");
        var replay = superadmin.call("POST", "/api/v1/admin/reports/" + reportId + "/actions",
                Map.of("actionType", "remove_post"));
        assertThat(body(replay).path("data").path("id").asString()).isEqualTo(actionId);

        var reporterActions = reporter.call("GET", DECISIONS + "/" + decisionId + "/actions", null);
        assertThat(reporterActions.statusCode()).isEqualTo(200);
        assertThat(body(reporterActions).path("data").size()).isEqualTo(1);
        assertThat(targetBrowser.call("GET", DECISIONS + "/" + decisionId + "/actions", null).statusCode()).isEqualTo(200);
        var stranger = login(account("user"));
        assertThat(stranger.call("GET", DECISIONS + "/" + decisionId + "/actions", null).statusCode()).isEqualTo(404);
    }

    @Test
    void legacyReviewedReportDoesNotInventAnOutcomeOrDeadline() throws Exception {
        var targetBrowser = login(other);
        String postId = approvedPost(targetBrowser);
        var reporter = login(owner);
        assertThat(reporter.call("POST", "/api/v1/reports",
                Map.of("targetType", "post", "targetId", postId, "reason", "旧流程举报")).statusCode())
                .isEqualTo(200);
        UUID reportId = reportId(owner.id(), UUID.fromString(postId));
        var superadmin = login(account("superadmin"));
        assertThat(superadmin.call("PUT", "/api/v1/admin/reports/" + reportId + "/review", null).statusCode())
                .isEqualTo(200);

        var mine = body(reporter.call("GET", REPORTS + "/" + reportId, null)).path("data");
        assertThat(mine.path("status").asString()).isEqualTo("legacy");
        assertThat(mine.path("outcome").isNull()).isTrue();
        assertThat(mine.path("decisionId").isNull()).isTrue();
        assertThat(body(reporter.call("GET", DECISIONS, null)).path("data").path("items").size()).isZero();
    }
}
