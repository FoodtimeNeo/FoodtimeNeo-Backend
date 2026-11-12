package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FavoriteGroupIT extends IntegrationApiSupport {
    private static final String GROUPS = "/api/v1/users/me/favorite-groups";

    private String groupId(tools.jackson.databind.JsonNode body) {
        return body.path("data").path("id").asString();
    }

    private Map<String, Object> group(String kind, String name) {
        return Map.of("kind", kind, "name", name);
    }

    @Test
    void dishGroupsFilterFavoritesAndKeepTheFavoriteAfterGroupDeletion() throws Exception {
        Browser owner = login(account("user"));
        assertThat(owner.call("PUT", "/api/v1/users/me/dish-favorites/" + dish, null).statusCode()).isEqualTo(200);

        var first = owner.call("POST", GROUPS, group("dish", " 学习 "));
        assertThat(first.statusCode()).isEqualTo(201);
        String firstId = groupId(body(first));
        var second = owner.call("POST", GROUPS, group("dish", "想吃"));
        String secondId = groupId(body(second));

        assertThat(owner.call("PUT", GROUPS + "/" + firstId + "/members/" + dish, null).statusCode()).isEqualTo(200);
        assertThat(owner.call("PUT", GROUPS + "/" + firstId + "/members/" + dish, null).statusCode()).isEqualTo(200);
        assertThat(owner.call("PUT", GROUPS + "/" + secondId + "/members/" + dish, null).statusCode()).isEqualTo(200);

        var membership = body(owner.call("GET", GROUPS + "/memberships?kind=dish&targetId=" + dish, null));
        assertThat(membership.path("data").path("groupIds").size()).isEqualTo(2);
        assertThat(body(owner.call("GET", "/api/v1/users/me/dish-favorites?groupId=" + firstId, null))
                .path("data").path("total").asInt()).isEqualTo(1);
        assertThat(body(owner.call("GET", "/api/v1/users/me/dish-favorites?unfiled=true", null))
                .path("data").path("total").asInt()).isZero();

        assertThat(owner.call("DELETE", GROUPS + "/" + firstId, null).statusCode()).isEqualTo(204);
        assertThat(body(owner.call("GET", "/api/v1/users/me/dish-favorites?groupId=" + secondId, null))
                .path("data").path("total").asInt()).isEqualTo(1);
        assertThat(body(owner.call("GET", "/api/v1/users/me/dish-favorites", null))
                .path("data").path("total").asInt()).isEqualTo(1);

        // Cancelling the favorite removes all group relations but keeps the group itself.
        assertThat(owner.call("DELETE", "/api/v1/users/me/dish-favorites/" + dish, null).statusCode()).isEqualTo(200);
        assertThat(body(owner.call("GET", GROUPS + "/memberships?kind=dish&targetId=" + dish, null))
                .path("data").path("groupIds").size()).isZero();
        assertThat(body(owner.call("GET", GROUPS + "?kind=dish", null)).path("data").size()).isEqualTo(1);

        var renamed = owner.call("PUT", GROUPS + "/" + secondId, Map.of("name", "学习"));
        assertThat(renamed.statusCode()).isEqualTo(200);
        assertThat(body(renamed).path("data").path("revision").asLong()).isEqualTo(2);
        var duplicate = owner.call("POST", GROUPS, group("dish", "学习"));
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(body(duplicate).path("code").asString()).isEqualTo("GROUP_NAME_TAKEN");
        var third = owner.call("POST", GROUPS, group("dish", "晚餐"));
        var renameConflict = owner.call("PUT", GROUPS + "/" + groupId(body(third)), Map.of("name", "学习"));
        assertThat(renameConflict.statusCode()).isEqualTo(409);
        assertThat(body(renameConflict).path("code").asString()).isEqualTo("GROUP_NAME_TAKEN");
    }

    @Test
    void postGroupsAreSeparateAndEnforceFavoriteOwnershipAndLimits() throws Exception {
        Browser author = login(owner);
        var payload = new HashMap<>(communityPost(author, "分组帖子"));
        author.call("POST", "/api/v1/community/posts", payload);
        var posts = body(author.call("GET", "/api/v1/users/me/community-posts?page=1&size=20", null))
                .path("data").path("items");
        String postId = posts.get(0).path("id").asString();
        // The post is pending in this fixture; groups still require a favorite, so use the dish
        // favorite path for ownership and only verify the post group's kind isolation.
        var postGroup = author.call("POST", GROUPS, group("post", "帖子收藏"));
        assertThat(postGroup.statusCode()).isEqualTo(201);
        assertThat(body(author.call("GET", GROUPS + "?kind=post", null)).path("data").size()).isEqualTo(1);
        assertThat(body(author.call("GET", GROUPS + "?kind=dish", null)).path("data").size()).isZero();
        assertThat(author.call("PUT", GROUPS + "/" + groupId(body(postGroup)) + "/members/" + postId, null)
                .statusCode()).isEqualTo(404);

        for (int index = 0; index < 19; index++) {
            assertThat(author.call("POST", GROUPS, group("post", "分组" + index)).statusCode()).isEqualTo(201);
        }
        var over = author.call("POST", GROUPS, group("post", "分组19"));
        assertThat(over.statusCode()).isEqualTo(409);
        assertThat(body(over).path("code").asString()).isEqualTo("GROUP_LIMIT_REACHED");
    }
}
