package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

/**
 * Community P2: comments submit → AI/human decision → list/delete;
 * admin comment queue carries AI verdicts. Fake AI: default LOW 0.9 stays
 * pending, "放心" auto-approves at 0.99, "违禁" auto-rejects at 0.95.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.ai.triage.enabled=true",
                "app.ai.triage.provider=fake",
                "app.ai.triage.initial-delay=PT30M"})
class CommunityCommentIT extends IntegrationApiSupport {

    @DynamicPropertySource
    static void aiEnabled(DynamicPropertyRegistry properties) {
        properties.add("app.ai.triage.enabled", () -> "true");
    }

    @Test
    void commentOnApprovedPostGoesThroughHumanQueue() throws Exception {
        Browser author = login(owner), viewer = login(other);
        UUID post = approvedPost(author, "主贴-" + UUID.randomUUID());

        var created = author.call("POST", "/api/v1/community/posts/" + post + "/comments",
                Map.of("content", "跟评-" + UUID.randomUUID()));
        assertThat(created.statusCode()).isEqualTo(202);
        UUID comment = UUID.fromString(body(created).path("data").path("id").asString());

        assertThat(listContents(viewer, post)).isEmpty();

        Browser admin = login(account("admin"));
        var items = body(admin.call("GET", "/api/v1/admin/community/comments?status=pending&size=100", null))
                .path("data").path("items");
        boolean seen = false;
        for (var item : items) {
            if (item.path("id").asString().equals(comment.toString())) {
                seen = true;
                assertThat(item.path("type").asString()).isEqualTo("community-comment");
                assertThat(item.path("aiRiskLevel").asString()).isEqualTo("LOW");
            }
        }
        assertThat(seen).isTrue();

        assertThat(admin.call("POST", "/api/v1/admin/community/comments/" + comment,
                Map.of("action", "approve")).statusCode()).isEqualTo(200);
        assertThat(listContents(viewer, post)).hasSize(1);
        assertThat(count(post)).isEqualTo(1);
    }

    @Test
    void confidentCommentIsAutoApprovedAndCounted() throws Exception {
        Browser author = login(owner);
        UUID post = approvedPost(author, "计数贴-" + UUID.randomUUID());
        String content = "放心跟评-" + UUID.randomUUID();
        assertThat(author.call("POST", "/api/v1/community/posts/" + post + "/comments",
                Map.of("content", content)).statusCode()).isEqualTo(202);

        String status = null;
        for (int i = 0; i < 30 && !"approved".equals(status); i++) {
            Thread.sleep(1000);
            status = jdbc.queryForObject("SELECT status FROM foodtime.community_comments WHERE content = ?",
                    String.class, content);
        }
        assertThat(status).isEqualTo("approved");
        assertThat(listContents(login(other), post)).contains(content);
        assertThat(count(post)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT actor FROM foodtime.content_moderation_logs WHERE community_comment_id IN"
                + " (SELECT id FROM foodtime.community_comments WHERE content = ?)",
                String.class, content)).isEqualTo("ai");
    }

    @Test
    void commentOnUnapprovedPostIsRejectedAndOnlyOwnerDeletes() throws Exception {
        Browser author = login(owner);
        var created = author.call("POST", "/api/v1/community/posts",
                Map.of("content", "未过审主贴-" + UUID.randomUUID()));
        UUID pending = UUID.fromString(body(created).path("data").path("id").asString());
        assertThat(author.call("POST", "/api/v1/community/posts/" + pending + "/comments",
                Map.of("content", "x")).statusCode()).isEqualTo(404);

        UUID post = approvedPost(author, "删评贴-" + UUID.randomUUID());
        var c2 = author.call("POST", "/api/v1/community/posts/" + post + "/comments",
                Map.of("content", "待删-" + UUID.randomUUID()));
        UUID comment = UUID.fromString(body(c2).path("data").path("id").asString());
        assertThat(login(other).call("DELETE", "/api/v1/community/comments/" + comment, null).statusCode()).isEqualTo(404);
        assertThat(author.call("DELETE", "/api/v1/community/comments/" + comment, null).statusCode()).isEqualTo(200);
        assertThat(author.call("POST", "/api/v1/community/posts", Map.of("content", "  ")).statusCode()).isEqualTo(400);
    }

    private UUID approvedPost(Browser author, String content) throws Exception {
        var created = author.call("POST", "/api/v1/community/posts", Map.of("content", content));
        assertThat(created.statusCode()).isEqualTo(202);
        UUID post = UUID.fromString(body(created).path("data").path("id").asString());
        Browser admin = login(account("admin"));
        assertThat(admin.call("POST", "/api/v1/admin/community/posts/" + post,
                Map.of("action", "approve")).statusCode()).isEqualTo(200);
        return post;
    }

    private java.util.List<String> listContents(Browser browser, UUID post) throws Exception {
        var items = body(browser.call("GET", "/api/v1/community/posts/" + post + "/comments?size=100", null))
                .path("data").path("items");
        var out = new java.util.ArrayList<String>();
        for (var item : items) out.add(item.path("content").asString());
        return out;
    }

    private long count(UUID post) {
        return jdbc.queryForObject("SELECT comment_count FROM foodtime.community_posts WHERE id = ?",
                Long.class, post);
    }
}
