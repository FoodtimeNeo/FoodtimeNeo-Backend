package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class DishImageIT extends ReadApiITSupport {
    private final List<UUID> imageIds = new ArrayList<>();
    @Override protected void cleanContentFixtures() {
        for (UUID id : imageIds) { jdbc.update("delete from foodtime.dish_images where id=?", id); }
    }
    private String path(UUID id) { return "/api/v1/dishes/" + id + "/images"; }
    private UUID image(UUID dish, UUID user, String status, String time) {
        UUID id = UUID.randomUUID();
        imageIds.add(id);
        jdbc.update("insert into foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status,created_at) values (?,?,?,?,?,?)",
                id, dish, user, "https://example.test/" + id + ".jpg", status, Timestamp.from(Instant.parse(time)));
        return id;
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin", "superadmin"})
    void approvedOnlyScopedToDishAndStablePagingForAllRoles(String role) throws Exception {
        UUID parent = stall(hall("active"), "active");
        UUID dish = dish(parent, "active", "1.00");
        UUID other = dish(parent, "active", "1.00");
        UUID uploader = user("user");
        UUID old = image(dish, uploader, "approved", "2026-01-01T00:00:00Z");
        UUID one = image(dish, uploader, "approved", "2026-01-02T00:00:00Z");
        UUID two = image(dish, uploader, "approved", "2026-01-02T00:00:00Z");
        UUID pending = image(dish, uploader, "pending", "2026-01-03T00:00:00Z");
        UUID rejected = image(dish, uploader, "rejected", "2026-01-03T00:00:00Z");
        UUID elsewhere = image(other, uploader, "approved", "2026-01-03T00:00:00Z");
        var browser = login(role);
        var response = browser.get(path(dish) + "?size=2&status=pending&dishId=" + other);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain(pending.toString(), rejected.toString(), elsewhere.toString(), uploader.toString());
        var data = json.readTree(response.body()).path("data");
        assertThat(data.path("total").asLong()).isEqualTo(3);
        assertThat(data.path("page").asInt()).isEqualTo(1);
        assertThat(data.path("size").asInt()).isEqualTo(2);
        var ties = new ArrayList<>(List.of(one, two));
        ties.sort(Comparator.comparing(UUID::toString));
        assertThat(data.path("items").get(0).path("id").asString()).isEqualTo(ties.get(0).toString());
        assertThat(data.path("items").get(1).path("id").asString()).isEqualTo(ties.get(1).toString());
        assertThat(data.path("items").get(0).size()).isEqualTo(4);
        assertThat(data.path("items").get(0).path("createdAt").asString()).isEqualTo("2026-01-02T00:00:00Z");
        var second = json.readTree(browser.get(path(dish) + "?page=2&size=2").body()).path("data");
        assertThat(second.path("items").size()).isEqualTo(1);
        assertThat(second.path("items").get(0).path("id").asString()).isEqualTo(old.toString());
        var beyond = json.readTree(browser.get(path(dish) + "?page=2147483647&size=100").body()).path("data");
        assertThat(beyond.path("items").size()).isZero();
        assertThat(beyond.path("total").asLong()).isEqualTo(3);
    }

    @Test
    void noImagesAndOnlyPendingImagesReturnEmptyPage() throws Exception {
        UUID dish = dish(stall(hall("active"), "active"), "active", "0.00");
        var browser = login("user");
        var data = json.readTree(browser.get(path(dish)).body()).path("data");
        assertThat(data.path("items").isArray()).isTrue();
        assertThat(data.path("total").asLong()).isZero();
        image(dish, userIds.get(0), "pending", "2026-01-01T00:00:00Z");
        assertThat(json.readTree(browser.get(path(dish)).body()).path("data").path("items").size()).isZero();
    }

    @Test
    void missingAndInvisibleParentsReturn404() throws Exception {
        var browser = login("admin");
        UUID one = dish(stall(hall("active"), "active"), "inactive", "1.00");
        UUID two = dish(stall(hall("active"), "inactive"), "active", "1.00");
        UUID three = dish(stall(hall("inactive"), "active"), "active", "1.00");
        for (UUID id : List.of(one, two, three)) {
            image(id, userIds.get(0), "approved", "2026-01-01T00:00:00Z");
        }
        for (UUID id : List.of(UUID.randomUUID(), one, two, three)) {
            var response = browser.get(path(id));
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("DISH_NOT_FOUND");
        }
    }

    @Test
    void subsequentRequestsReflectApprovalAndParentChanges() throws Exception {
        UUID hall = hall("active");
        UUID dish = dish(stall(hall, "active"), "active", "1.00");
        var browser = login("user");
        UUID image = image(dish, userIds.get(0), "pending", "2026-01-01T00:00:00Z");
        assertThat(json.readTree(browser.get(path(dish)).body()).path("data").path("total").asLong()).isZero();
        jdbc.update("update foodtime.dish_images set status='approved' where id=?", image);
        assertThat(browser.get(path(dish)).body()).contains(image.toString());
        jdbc.update("update foodtime.dish_images set status='rejected' where id=?", image);
        assertThat(json.readTree(browser.get(path(dish)).body()).path("data").path("total").asLong()).isZero();
        jdbc.update("update foodtime.dining_halls set status='inactive' where id=?", hall);
        assertThat(browser.get(path(dish)).statusCode()).isEqualTo(404);
    }

    @Test
    void enforcesLoginPagingAndDisabledAccounts() throws Exception {
        assertThat(new Browser().get(path(UUID.randomUUID())).statusCode()).isEqualTo(401);
        var browser = login("user");
        assertThat(browser.get(path(UUID.randomUUID()) + "?size=101").statusCode()).isEqualTo(400);
        assertThat(browser.get("/api/v1/dishes/bad-id/images").statusCode()).isEqualTo(400);
        jdbc.update("update foodtime.users set status='disabled' where id=?", userIds.get(0));
        assertThat(browser.get(path(UUID.randomUUID())).statusCode()).isEqualTo(401);
    }

    @Test
    void openApiDocumentsPageAndPublicImageFields() throws Exception {
        var document = json.readTree(new Browser().get("/v3/api-docs").body());
        var get = document.path("paths").path("/api/v1/dishes/{dishId}/images").path("get");
        assertThat(get.path("security").get(0).has("sessionCookie")).isTrue();
        assertThat(get.path("parameters").size()).isEqualTo(3);
        var properties = document.path("components").path("schemas").path("DishImageResponse").path("properties");
        assertThat(properties.size()).isEqualTo(4);
        assertThat(properties.has("imageUrl")).isTrue();
        assertThat(properties.has("uploadedByUserId")).isFalse();
    }
}
