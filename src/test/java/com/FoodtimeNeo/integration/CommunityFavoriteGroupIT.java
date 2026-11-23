package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityFavoriteGroupIT extends IntegrationApiSupport {
    private static final String SAVED = "/api/v1/users/me/community-favorites";
    private static final String GROUPS = "/api/v1/users/me/favorite-groups";

    private UUID post() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO foodtime.community_posts
                  (id,user_id,title,content,status,review_stage,reviewed_by_user_id,reviewed_at)
                VALUES (?,?,'收藏分组回归','收藏分组回归','approved','completed',?,now())
                """, id, owner.id(), other.id());
        return id;
    }

    private void favorite(Account actor, UUID post, int order) {
        jdbc.update("""
                INSERT INTO foodtime.community_favorites(user_id,post_id,created_at)
                VALUES (?,?,now() + (? * interval '1 second'))
                """, actor.id(), post, order);
    }

    private String group(Browser browser, String name) throws Exception {
        var response = browser.call("POST", GROUPS, Map.of("kind", "post", "name", name));
        assertThat(response.statusCode()).isEqualTo(201);
        return body(response).path("data").path("id").asString();
    }

    private void add(Browser browser, String group, UUID post) throws Exception {
        assertThat(browser.call("PUT", GROUPS + "/" + group + "/members/" + post, null).statusCode())
                .isEqualTo(200);
    }

    private JsonNode saved(Browser browser, String query, int total, UUID... expected) throws Exception {
        var response = browser.call("GET", SAVED + query, null);
        assertThat(response.statusCode()).as("收藏筛选 %s: %s", query, response.body()).isEqualTo(200);
        var data = body(response).path("data");
        assertThat(data.path("total").asInt()).isEqualTo(total);
        var items = data.path("items");
        assertThat(items.size()).isEqualTo(expected.length);
        for (int index = 0; index < expected.length; index++) {
            assertThat(items.get(index).path("id").asString()).isEqualTo(expected[index].toString());
            assertThat(items.get(index).path("favoritedByMe").asBoolean()).isTrue();
        }
        return data;
    }

    @ParameterizedTest
    @ValueSource(strings = {"all", "default", "custom"})
    void eachViewReturnsOwnFavoritesWithConsistentPagingAndLikeState(String view) throws Exception {
        Browser browser = login(owner), second = login(other);
        UUID first = post(), last = post(), outside = post();
        favorite(owner, first, 1);
        favorite(owner, last, 2);
        favorite(owner, outside, 3);
        favorite(other, first, 1);
        favorite(other, outside, 2);
        String ownGroup = group(browser, "常看"), otherGroup = group(second, "另一个账号");
        add(second, otherGroup, first);
        // A different account's membership must not remove this user's default favorite.
        String filter;
        if (view.equals("custom")) {
            add(browser, ownGroup, first);
            add(browser, ownGroup, last);
            String duplicateGroup = group(browser, "也想看");
            add(browser, duplicateGroup, last);
            filter = "?groupId=" + ownGroup;
        } else {
            add(browser, ownGroup, outside);
            filter = view.equals("default") ? "?unfiled=true" : "?unfiled=false";
        }
        jdbc.update("INSERT INTO foodtime.community_post_likes(user_id,post_id) VALUES (?,?)", owner.id(), last);
        int total = view.equals("all") ? 3 : 2;
        UUID newest = view.equals("all") ? outside : last;
        JsonNode page = saved(browser, filter + "&size=1&page=1", total, newest);
        assertThat(page.path("page").asInt()).isEqualTo(1);
        assertThat(page.path("size").asInt()).isEqualTo(1);
        UUID next = view.equals("all") ? last : first;
        var secondPage = saved(browser, filter + "&size=1&page=2", total, next);
        assertThat(secondPage.path("items").get(0).path("likedByMe").asBoolean()).isEqualTo(next.equals(last));
        saved(browser, filter + "&size=1&page=" + (total + 1), total);
        saved(second, "?groupId=" + ownGroup, 0);
        saved(browser, "?groupId=" + otherGroup, 0);
    }

    @Test
    void defaultAndCustomGroupsRespectVisibilityAndDeletedGroupsReturnFavoritesToDefault() throws Exception {
        Browser browser = login(owner);
        UUID visible = post(), pending = post(), deleted = post();
        for (UUID id : new UUID[]{visible, pending, deleted}) favorite(owner, id, 1);
        String group = group(browser, "测试收藏夹");
        for (UUID id : new UUID[]{visible, pending, deleted}) add(browser, group, id);
        jdbc.update("""
                UPDATE foodtime.community_posts SET content_revision=content_revision+1,edited_at=now(),
                  status='pending',review_stage='manual_pending',manual_review_reason='ai_disabled',
                  reviewed_at=NULL,reviewed_by_user_id=NULL,reviewed_by_ai_model=NULL,review_reason=NULL
                WHERE id=?
                """, pending);
        jdbc.update("UPDATE foodtime.community_posts SET deleted_at=now() WHERE id=?", deleted);
        saved(browser, "?groupId=" + group, 1, visible);
        saved(browser, "?unfiled=true", 0);
        assertThat(browser.call("DELETE", GROUPS + "/" + group, null).statusCode()).isEqualTo(204);
        saved(browser, "?unfiled=true", 1, visible);
        saved(browser, "?groupId=" + group, 0);
        saved(browser, "?groupId=" + UUID.randomUUID(), 0);
        assertThat(new Browser().call("GET", SAVED + "?unfiled=true", null).statusCode()).isEqualTo(401);
    }
}
