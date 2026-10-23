package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

/**
 * Community P1: submit → AI/human decision → feed/mine/detail/delete.
 * Fake AI: default LOW 0.9 stays pending, "放心" auto-approves at 0.99.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.ai.triage.enabled=true",
                "app.ai.triage.provider=fake",
                "app.ai.triage.initial-delay=PT30M"})
class CommunityPostIT extends IntegrationApiSupport {

    @DynamicPropertySource
    static void aiEnabled(DynamicPropertyRegistry properties) {
        // Beats the hermetic false from IntegrationApiSupport and any local .env (same as AiTriageIT).
        properties.add("app.ai.triage.enabled", () -> "true");
    }

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.FoodtimeNeo.ai.service.FakeAiProvider provider;
    @org.junit.jupiter.api.BeforeEach
    void cleanImages() {
        org.mockito.Mockito.doReturn(new com.FoodtimeNeo.ai.dto.AiVerdict("LOW",0.99,java.util.List.of(),"测试图片通过","fake"))
            .when(provider).checkImage(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void pendingPostIsInvisibleUntilHumanApproves() throws Exception {
        Browser author = login(owner), viewer = login(other);
        var created = author.call("POST", "/api/v1/community/posts", communityPost(author,"社区首贴-" + UUID.randomUUID()));
        assertThat(created.statusCode()).isEqualTo(202);
        UUID id = UUID.fromString(body(created).path("data").path("id").asString());

        assertThat(viewer.call("GET", "/api/v1/community/feed", null).statusCode()).isEqualTo(200);
        assertThat(feedIds(viewer)).doesNotContain(id.toString());
        assertThat(viewer.call("GET", "/api/v1/community/posts/" + id, null).statusCode()).isEqualTo(404);

        Browser admin = login(account("admin"));
        assertThat(admin.call("POST", "/api/v1/admin/community/posts/" + id,
                Map.of("action", "approve")).statusCode()).isEqualTo(200);

        assertThat(feedIds(viewer)).contains(id.toString());
        assertThat(body(viewer.call("GET", "/api/v1/community/posts/" + id, null))
                .path("data").path("userDisplayName").asString()).isEqualTo("integration user");
    }

    @Test
    void confidentPostIsAutoApprovedAndLoggedAsAi() throws Exception {
        Browser author = login(owner);
        String content = "放心-" + UUID.randomUUID();
        assertThat(author.call("POST", "/api/v1/community/posts", communityPost(author,content)).statusCode()).isEqualTo(202);

        String status = null;
        for (int i = 0; i < 30 && !"approved".equals(status); i++) {
            Thread.sleep(1000);
            status = jdbc.queryForObject("SELECT status FROM foodtime.community_posts WHERE content = ?",
                    String.class, content);
        }
        assertThat(status).isEqualTo("approved");
        assertThat(feedContents(login(other))).contains(content);
        assertThat(jdbc.queryForObject("SELECT actor FROM foodtime.content_moderation_logs WHERE community_post_id IN"
                + " (SELECT id FROM foodtime.community_posts WHERE content = ?)",
                String.class, content)).isEqualTo("ai");
    }

    @Test
    void adminQueueCarriesAiVerdictAndMineShowsOwnStates() throws Exception {
        Browser author = login(owner);
        String content = "待审-" + UUID.randomUUID();
        assertThat(author.call("POST", "/api/v1/community/posts", communityPost(author,content)).statusCode()).isEqualTo(202);

        Browser admin = login(account("admin"));
        var items = body(admin.call("GET", "/api/v1/admin/community/posts?status=pending&size=100", null))
                .path("data").path("items");
        boolean seen = false;
        for (var item : items) {
            if (item.path("content").asString().equals(content)) {
                seen = true;
                assertThat(item.path("aiRiskLevel").asString()).isEqualTo("LOW");
                assertThat(item.path("type").asString()).isEqualTo("community-post");
            }
        }
        assertThat(seen).isTrue();

        var mine = body(author.call("GET", "/api/v1/users/me/community-posts?size=100", null))
                .path("data").path("items");
        boolean own = false;
        for (var item : mine) {
            if (item.path("content").asString().equals(content)) {
                own = true;
                assertThat(item.path("status").asString()).isEqualTo("pending");
            }
        }
        assertThat(own).isTrue();
    }

    @Test
    void emptyContentIsRejectedAndOthersCannotDelete() throws Exception {
        Browser author = login(owner);
        assertThat(author.call("POST", "/api/v1/community/posts", communityPost(author,"  ")).statusCode()).isEqualTo(400);
        var created = author.call("POST", "/api/v1/community/posts", communityPost(author,"删帖-" + UUID.randomUUID()));
        UUID id = UUID.fromString(body(created).path("data").path("id").asString());
        assertThat(login(other).call("DELETE", "/api/v1/community/posts/" + id, null).statusCode()).isEqualTo(404);
        assertThat(author.call("DELETE", "/api/v1/community/posts/" + id, null).statusCode()).isEqualTo(200);
    }

    private java.util.List<String> feedIds(Browser browser) throws Exception {
        var items = body(browser.call("GET", "/api/v1/community/feed?size=100", null)).path("data").path("items");
        var ids = new java.util.ArrayList<String>();
        for (var item : items) ids.add(item.path("id").asString());
        return ids;
    }

    private java.util.List<String> feedContents(Browser browser) throws Exception {
        var items = body(browser.call("GET", "/api/v1/community/feed?size=100", null)).path("data").path("items");
        var contents = new java.util.ArrayList<String>();
        for (var item : items) contents.add(item.path("content").asString());
        return contents;
    }
}
