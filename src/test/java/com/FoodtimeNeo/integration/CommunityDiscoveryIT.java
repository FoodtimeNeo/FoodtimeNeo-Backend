package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityDiscoveryIT extends IntegrationApiSupport {
    private static final String FEED = "/api/v1/community/feed/cursor";

    @Test void searchFollowingAndAuthorResultsSharePublicVisibilityAndKeepTheirOwnCursor() throws Exception {
        Account follower = account("user");
        Browser author = login(owner), secondAuthor = login(other), viewer = login(follower);
        Browser admin = login(account("admin"));
        String keyword = "食记" + UUID.randomUUID().toString().substring(0, 8);
        String first = post(author, keyword + "MaTcH%_一", admin);
        String second = post(author, keyword + "MaTcH%_二", admin);
        String third = post(secondAuthor, keyword + "MaTcH%_三", admin);
        post(secondAuthor, keyword + "matchXX", admin);
        post(author, keyword + "match%_待审", null);
        assertThat(viewer.call("PUT", "/api/v1/users/" + owner.id() + "/follow", null).statusCode()).isEqualTo(200);

        String search = FEED + "?q=" + URLEncoder.encode(keyword + "match%_", StandardCharsets.UTF_8);
        var data = body(viewer.call("GET", search, null)).path("data");
        assertThat(data.path("total").asLong()).isEqualTo(3);
        assertThat(data.path("items")).hasSize(3);
        assertThat(data.path("items").get(0).path("id").asString()).isEqualTo(third);
        String following = search + "&following=true&size=1";
        data = body(viewer.call("GET", following, null)).path("data");
        assertThat(data.path("total").asLong()).isEqualTo(2);
        assertThat(data.path("items").get(0).path("id").asString()).isEqualTo(second);
        String cursor = data.path("nextCursor").asString();
        assertThat(body(viewer.call("GET", following + "&cursor=" + cursor, null)).path("data")
                .path("items").get(0).path("id").asString()).isEqualTo(first);
        assertThat(viewer.call("GET", search + "&cursor=" + cursor, null).statusCode()).isEqualTo(400);
        assertThat(secondAuthor.call("GET", following + "&cursor=" + cursor, null).statusCode()).isEqualTo(400);

        String profile = "/api/v1/community/authors/" + owner.id();
        assertThat(new Browser().call("GET", profile, null).statusCode()).isEqualTo(401);
        var authorData = body(viewer.call("GET", profile, null)).path("data");
        assertThat(authorData.path("id").asString()).isEqualTo(owner.id().toString());
        assertThat(authorData.has("email")).isFalse();
        assertThat(body(viewer.call("GET", profile + "/posts", null)).path("data").path("items")).hasSize(2);

        assertThat(viewer.call("DELETE", "/api/v1/users/" + owner.id() + "/follow", null).statusCode()).isEqualTo(200);
        assertThat(body(viewer.call("GET", following, null)).path("data").path("items")).isEmpty();
        jdbc.update("UPDATE foodtime.users SET status='disabled' WHERE id=?", owner.id());
        assertThat(viewer.call("GET", profile, null).statusCode()).isEqualTo(404);
        assertThat(viewer.call("GET", profile + "/posts", null).statusCode()).isEqualTo(404);
    }

    private String post(Browser author, String content, Browser moderator) throws Exception {
        var created = author.call("POST", "/api/v1/community/posts", communityPost(author, content));
        assertThat(created.statusCode()).isEqualTo(202);
        String id = body(created).path("data").path("id").asString();
        if (moderator != null) {
            assertThat(moderator.call("POST", "/api/v1/admin/community/posts/" + id, Map.of("action", "approve"))
                    .statusCode()).isEqualTo(200);
        }
        return id;
    }
}
