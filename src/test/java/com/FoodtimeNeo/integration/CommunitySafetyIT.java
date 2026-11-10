package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunitySafetyIT extends IntegrationApiSupport {
    private static final String CONVERSATIONS = "/api/v1/users/me/conversations";
    private static final String REPORTS = "/api/v1/reports";
    private static final String QUEUE = "/api/v1/admin/reports";

    @Test void privateBlockStopsBothDirectionsAndOnlyOwnerCanUndoIt() throws Exception {
        Browser author = login(owner), receiver = login(other);
        String messages = CONVERSATIONS + "/" + start(author, other) + "/messages";
        assertThat(send(author, messages, "拉黑前").statusCode()).isEqualTo(200);
        assertThat(receiver.call("PUT", block(owner), null).statusCode()).isEqualTo(200);
        assertThat(receiver.call("PUT", block(owner), null).statusCode()).isEqualTo(200);
        assertThat(body(author.call("GET", block(other), null)).path("data").path("blocked").asBoolean()).isFalse();
        assertThat(body(receiver.call("GET", block(owner), null)).path("data").path("blocked").asBoolean()).isTrue();
        assertThat(send(author, messages, "禁止消息").statusCode()).isEqualTo(403);
        assertThat(send(receiver, messages, "禁止回复").statusCode()).isEqualTo(403);
        assertThat(author.call("POST", CONVERSATIONS, Map.of("userId", other.id())).statusCode()).isEqualTo(403);
        assertThat(author.call("DELETE", block(other), null).statusCode()).isEqualTo(200);
        assertThat(send(author, messages, "仍被拉黑").statusCode()).isEqualTo(403);
        assertThat(body(receiver.call("GET", messages, null)).path("data").path("items")).hasSize(1);
        assertThat(receiver.call("DELETE", block(owner), null).statusCode()).isEqualTo(200);
        assertThat(send(author, messages, "已解除").statusCode()).isEqualTo(200);
    }

    @Test void onlyRecipientCanReportOnePrivateMessageAndOnlySuperadminSeesEvidence() throws Exception {
        Account visitor = account("visitor");
        Browser author = login(owner), receiver = login(visitor), outsider = login(other);
        String messages = CONVERSATIONS + "/" + start(author, visitor) + "/messages";
        String messageId = body(send(author, messages, "只举报这一条")).path("data").path("id").asString();
        assertThat(send(author, messages, "同一会话的其他消息").statusCode()).isEqualTo(200);
        var payload = Map.of("targetType", "message", "targetId", messageId, "reason", "骚扰");
        assertThat(new Browser().call("POST", REPORTS, payload).statusCode()).isIn(401, 403);
        assertThat(author.call("POST", REPORTS, payload).statusCode()).isEqualTo(404);
        assertThat(outsider.call("POST", REPORTS, payload).statusCode()).isEqualTo(404);
        assertThat(receiver.call("POST", REPORTS, payload).statusCode()).isEqualTo(200);
        assertThat(receiver.call("POST", REPORTS, payload).statusCode()).isEqualTo(200);
        assertThat(receiver.call("PUT", block(owner), null).statusCode()).isEqualTo(200);
        assertThat(send(author, messages, "不能继续骚扰").statusCode()).isEqualTo(403);
        assertThat(receiver.call("GET", QUEUE, null).statusCode()).isEqualTo(403);
        assertThat(login(account("admin")).call("GET", QUEUE, null).statusCode()).isEqualTo(403);
        Browser superadmin = login(account("superadmin"));
        var queue = body(superadmin.call("GET", QUEUE, null)).path("data").path("items");
        var evidence = queue.get(0);
        assertThat(queue).hasSize(1);
        assertThat(evidence.path("targetId").asString()).isEqualTo(messageId);
        assertThat(evidence.path("excerpt").asString()).isEqualTo("只举报这一条");
        assertThat(evidence.has("reporterId")).isFalse();
        assertThat(superadmin.call("PUT", QUEUE + "/" + evidence.path("id").asString() + "/review", null)
                .statusCode()).isEqualTo(200);
        assertThat(body(superadmin.call("GET", QUEUE, null)).path("data").path("items")).isEmpty();
    }

    @Test void validatesPublicReportAgainstApprovedContent() throws Exception {
        Browser author = login(owner), reporter = login(other), admin = login(account("admin"));
        String postId = body(author.call("POST", "/api/v1/community/posts", communityPost(author, "举报测试帖子")))
                .path("data").path("id").asString();
        var report = Map.of("targetType", "post", "targetId", postId, "reason", "需要审核");
        assertThat(reporter.call("POST", REPORTS, report).statusCode()).isEqualTo(404);
        assertThat(admin.call("POST", "/api/v1/admin/community/posts/" + postId, Map.of("action", "approve"))
                .statusCode()).isEqualTo(200);
        assertThat(author.call("POST", REPORTS, report).statusCode()).isEqualTo(404);
        assertThat(reporter.call("POST", REPORTS, report).statusCode()).isEqualTo(200);
    }

    private String block(Account account) {
        return "/api/v1/users/" + account.id() + "/block";
    }

    private String start(Browser browser, Account recipient) throws Exception {
        return body(browser.call("POST", CONVERSATIONS, Map.of("userId", recipient.id()))).path("data").path("id").asString();
    }

    private java.net.http.HttpResponse<String> send(Browser browser, String path, String content) throws Exception {
        return browser.call("POST", path, Map.of("content", content, "clientRequestId", UUID.randomUUID()));
    }
}
