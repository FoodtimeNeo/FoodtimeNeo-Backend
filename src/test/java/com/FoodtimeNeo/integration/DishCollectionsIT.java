package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DishCollectionsIT extends IntegrationApiSupport {
    private static final String SAVED = "/api/v1/users/me/dish-favorites";

    @Test void favoritesArePrivateIdempotentAndOnlyListVisibleDishes() throws Exception {
        Browser first = login(owner), second = login(other), visitor = login(account("visitor"));
        String path = SAVED + "/" + dish;
        assertThat(new Browser().call("GET", SAVED, null).statusCode()).isEqualTo(401);
        assertThat(visitor.call("PUT", path, null).statusCode()).isEqualTo(403);
        assertThat(first.call("PUT", path, null).statusCode()).isEqualTo(200);
        assertThat(first.call("PUT", path, null).statusCode()).isEqualTo(200);
        assertThat(body(first.call("GET", path, null)).path("data").path("saved").asBoolean()).isTrue();
        assertThat(body(second.call("GET", path, null)).path("data").path("saved").asBoolean()).isFalse();
        assertThat(body(second.call("GET", SAVED, null)).path("data").path("items")).isEmpty();
        var row = body(first.call("GET", SAVED, null)).path("data").path("items");
        assertThat(row).hasSize(1);
        assertThat(row.get(0).path("id").asString()).isEqualTo(dish.toString());
        assertThat(row.get(0).path("stallId").asString()).isEqualTo(stall.toString());
        assertThat(second.call("DELETE", path, null).statusCode()).isEqualTo(200);
        assertThat(body(first.call("GET", path, null)).path("data").path("saved").asBoolean()).isTrue();
        jdbc.update("UPDATE foodtime.stalls SET status='inactive' WHERE id=?", stall);
        assertThat(body(first.call("GET", SAVED, null)).path("data").path("items")).isEmpty();
        assertThat(first.call("PUT", path, null).statusCode()).isEqualTo(404);
        assertThat(first.call("DELETE", path, null).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE foodtime.stalls SET status='active' WHERE id=?", stall);
        assertThat(body(first.call("GET", path, null)).path("data").path("saved").asBoolean()).isFalse();
    }

    @Test void linkedDishHonorsVisibilityAndPublicationIdempotency() throws Exception {
        Browser author = login(owner), reader = login(other), moderator = login(account("admin"));
        String posts = "/api/v1/community/posts";
        var payload = new HashMap<>(communityPost(author, "想再吃一次"));
        payload.put("dishId", UUID.randomUUID());
        assertThat(author.call("POST", posts, payload).statusCode()).isEqualTo(404);
        payload.put("dishId", dish);
        var submitted = author.call("POST", posts, payload);
        assertThat(submitted.statusCode()).isEqualTo(202);
        String post = body(submitted).path("data").path("id").asString();
        assertThat(body(author.call("POST", posts, payload)).path("data").path("id").asString()).isEqualTo(post);
        payload.remove("dishId");
        assertThat(author.call("POST", posts, payload).statusCode()).isEqualTo(409);
        assertThat(reader.call("GET", posts + "/" + post, null).statusCode()).isEqualTo(404);
        assertThat(moderator.call("POST", "/api/v1/admin/community/posts/" + post, Map.of("action", "approve"))
                .statusCode()).isEqualTo(200);
        var dishLink = body(reader.call("GET", posts + "/" + post, null)).path("data").path("linkedDish");
        assertThat(dishLink.path("id").asString()).isEqualTo(dish.toString());
        assertThat(dishLink.path("diningHallId").asString()).isEqualTo(hall.toString());
        jdbc.update("UPDATE foodtime.dining_halls SET status='inactive' WHERE id=?", hall);
        assertThat(body(reader.call("GET", posts + "/" + post, null)).path("data").path("linkedDish").isNull()).isTrue();
    }
}
