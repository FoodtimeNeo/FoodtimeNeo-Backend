package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

/**
 * Community P3: like/favorite toggles with denormalized counts,
 * repost creates a pending post linked to its origin.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.ai.triage.enabled=true",
                "app.ai.triage.provider=fake",
                "app.ai.triage.initial-delay=PT30M"})
class CommunityInteractionIT extends IntegrationApiSupport {

    @DynamicPropertySource
    static void aiEnabled(DynamicPropertyRegistry properties) {
        properties.add("app.ai.triage.enabled", () -> "true");
    }

    @Test
    void likeTogglesAndCountsFollow() throws Exception {
        Browser author = login(owner), viewer = login(other);
        UUID post = approvedPost(author, "点赞贴-" + UUID.randomUUID());

        var liked = viewer.call("POST", "/api/v1/community/likes",
                Map.of("targetType", "post", "targetId", post.toString()));
        assertThat(liked.statusCode()).isEqualTo(200);
        assertThat(body(liked).path("data").path("liked").asBoolean()).isTrue();
        assertThat(body(liked).path("data").path("likeCount").asInt()).isEqualTo(1);
        assertThat(detail(viewer, post).path("likedByMe").asBoolean()).isTrue();

        var unliked = viewer.call("POST", "/api/v1/community/likes",
                Map.of("targetType", "post", "targetId", post.toString()));
        assertThat(body(unliked).path("data").path("liked").asBoolean()).isFalse();
        assertThat(body(unliked).path("data").path("likeCount").asInt()).isEqualTo(0);

        assertThat(viewer.call("POST", "/api/v1/community/likes",
                Map.of("targetType", "video", "targetId", post.toString())).statusCode()).isEqualTo(400);
    }

    @Test
    void favoriteTogglesAndMineListsIt() throws Exception {
        Browser author = login(owner);
        UUID post = approvedPost(author, "收藏贴-" + UUID.randomUUID());

        var fav = author.call("POST", "/api/v1/community/favorites", Map.of("postId", post.toString()));
        assertThat(body(fav).path("data").path("favorited").asBoolean()).isTrue();
        assertThat(mineFavorites(author)).contains(post.toString());

        var unfav = author.call("POST", "/api/v1/community/favorites", Map.of("postId", post.toString()));
        assertThat(body(unfav).path("data").path("favorited").asBoolean()).isFalse();
        assertThat(mineFavorites(author)).doesNotContain(post.toString());
    }

    @Test
    void repostLinksOriginAndBumpsCountOnApprove() throws Exception {
        Browser author = login(owner), reposter = login(other);
        UUID origin = approvedPost(author, "原贴-" + UUID.randomUUID());

        var created = reposter.call("POST", "/api/v1/community/posts/" + origin + "/reposts",
                Map.of("clientRequestId", UUID.randomUUID(), "content", "转评-" + UUID.randomUUID()));
        assertThat(created.statusCode()).isEqualTo(202);
        UUID repost = UUID.fromString(body(created).path("data").path("id").asString());

        Browser admin = login(account("admin"));
        assertThat(admin.call("POST", "/api/v1/admin/community/posts/" + repost,
                Map.of("action", "approve")).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT repost_count FROM foodtime.community_posts WHERE id = ?",
                Long.class, origin)).isEqualTo(1L);
        assertThat(detail(reposter, repost).path("repostOfId").asString()).isEqualTo(origin.toString());
    }

    @Test
    void likeOnPendingPostIsNotFound() throws Exception {
        Browser author = login(owner);
        var created = author.call("POST", "/api/v1/community/posts", communityPost(author,"待审被赞-" + UUID.randomUUID()));
        UUID pending = UUID.fromString(body(created).path("data").path("id").asString());
        assertThat(author.call("POST", "/api/v1/community/likes",
                Map.of("targetType", "post", "targetId", pending.toString())).statusCode()).isEqualTo(200);
        assertThat(login(other).call("POST", "/api/v1/community/likes",
                Map.of("targetType", "post", "targetId", pending.toString())).statusCode()).isEqualTo(404);
    }

    private UUID approvedPost(Browser author, String content) throws Exception {
        var created = author.call("POST", "/api/v1/community/posts", communityPost(author,content));
        assertThat(created.statusCode()).isEqualTo(202);
        UUID post = UUID.fromString(body(created).path("data").path("id").asString());
        Browser admin = login(account("admin"));
        assertThat(admin.call("POST", "/api/v1/admin/community/posts/" + post,
                Map.of("action", "approve")).statusCode()).isEqualTo(200);
        return post;
    }

    private tools.jackson.databind.JsonNode detail(Browser browser, UUID post) throws Exception {
        return body(browser.call("GET", "/api/v1/community/posts/" + post, null)).path("data");
    }

    private java.util.List<String> mineFavorites(Browser browser) throws Exception {
        var items = body(browser.call("GET", "/api/v1/users/me/community-favorites?size=100", null))
                .path("data").path("items");
        var ids = new java.util.ArrayList<String>();
        for (var item : items) ids.add(item.path("id").asString());
        return ids;
    }
}
