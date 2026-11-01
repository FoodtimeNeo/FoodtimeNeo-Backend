package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FollowIT extends IntegrationApiSupport {
    @Test
    void followIsIdempotentAndVisibleFromBothSides() throws Exception {
        Browser follower = login(owner);
        String target = "/api/v1/users/" + other.id() + "/follow";

        var first = follower.call("PUT", target, null);
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(body(first).path("data").path("followed").asBoolean()).isTrue();
        assertThat(body(first).path("data").path("followerCount").asLong()).isEqualTo(1L);

        // Repeating keeps a single relation.
        var repeat = follower.call("PUT", target, null);
        assertThat(repeat.statusCode()).isEqualTo(200);
        assertThat(body(repeat).path("data").path("followerCount").asLong()).isEqualTo(1L);

        var status = follower.call("GET", "/api/v1/users/" + other.id() + "/follow-status", null);
        assertThat(body(status).path("data").path("followed").asBoolean()).isTrue();

        var followers = follower.call("GET", "/api/v1/users/" + other.id() + "/followers?page=1&size=20", null);
        assertThat(body(followers).path("data").path("total").asLong()).isEqualTo(1L);
        assertThat(body(followers).path("data").path("items").get(0).path("id").asString())
                .isEqualTo(owner.id().toString());

        var following = follower.call("GET", "/api/v1/users/" + owner.id() + "/following?page=1&size=20", null);
        assertThat(body(following).path("data").path("items").get(0).path("id").asString())
                .isEqualTo(other.id().toString());
    }

    @Test
    void profileExposesFollowCounts() throws Exception {
        Browser follower = login(owner);
        follower.call("PUT", "/api/v1/users/" + other.id() + "/follow", null);

        var mine = follower.call("GET", "/api/v1/users/me/profile", null);
        assertThat(body(mine).path("data").path("followingCount").asLong()).isEqualTo(1L);
        assertThat(body(mine).path("data").path("followerCount").asLong()).isEqualTo(0L);

        var theirs = login(other).call("GET", "/api/v1/users/me/profile", null);
        assertThat(body(theirs).path("data").path("followerCount").asLong()).isEqualTo(1L);
    }

    @Test
    void selfFollowAndUnknownTargetAreRejected() throws Exception {
        Browser follower = login(owner);
        assertThat(follower.call("PUT", "/api/v1/users/" + owner.id() + "/follow", null).statusCode())
                .isEqualTo(400);
        assertThat(follower.call("PUT", "/api/v1/users/" + java.util.UUID.randomUUID() + "/follow", null)
                .statusCode()).isEqualTo(404);
    }

    @Test
    void unfollowRemovesTheRelation() throws Exception {
        Browser follower = login(owner);
        String target = "/api/v1/users/" + other.id() + "/follow";
        follower.call("PUT", target, null);

        var removed = follower.call("DELETE", target, null);
        assertThat(removed.statusCode()).isEqualTo(200);
        assertThat(body(removed).path("data").path("followed").asBoolean()).isFalse();
        assertThat(body(removed).path("data").path("followerCount").asLong()).isEqualTo(0L);
    }

    @Test
    void listsRequireAuthentication() throws Exception {
        Browser anonymous = new Browser();
        assertThat(anonymous.call("GET", "/api/v1/users/" + other.id() + "/followers", null).statusCode())
                .isEqualTo(401);
    }
}
