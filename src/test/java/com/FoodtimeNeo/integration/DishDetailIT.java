package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class DishDetailIT extends ReadApiITSupport {
    private String path(UUID id) { return "/api/v1/dishes/" + id; }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin", "superadmin"})
    void returnsJoinedContextNullDescriptionAndExactPriceForAllRoles(String role) throws Exception {
        UUID hall = hall("active");
        UUID stall = stall(hall, "active");
        UUID dish = dish(stall, "active", "99999999.99");
        var response = login(role).get(path(dish));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Request-Id")).isNotEmpty();
        var data = json.readTree(response.body()).path("data");
        assertThat(data.size()).isEqualTo(8);
        assertThat(data.path("id").asString()).isEqualTo(dish.toString());
        assertThat(data.path("stallId").asString()).isEqualTo(stall.toString());
        assertThat(data.path("diningHallId").asString()).isEqualTo(hall.toString());
        assertThat(data.path("stallName").asString()).isEqualTo("查询测试档口-" + stall);
        assertThat(data.path("diningHallName").asString()).isEqualTo("查询测试食堂-" + hall);
        assertThat(data.has("description")).isTrue();
        assertThat(data.path("description").isNull()).isTrue();
        assertThat(data.path("price").isNumber()).isTrue();
        assertThat(new BigDecimal(data.path("price").asString())).isEqualByComparingTo("99999999.99");
    }

    @Test
    void missingOrAnyInactiveAncestorReturns404RegardlessOfQueryOverrides() throws Exception {
        UUID hiddenDish = dish(stall(hall("active"), "active"), "inactive", "1.00");
        UUID hiddenStallDish = dish(stall(hall("active"), "inactive"), "active", "1.00");
        UUID hiddenHallDish = dish(stall(hall("inactive"), "active"), "active", "1.00");
        var browser = login("admin");
        for (UUID id : new UUID[]{UUID.randomUUID(), hiddenDish, hiddenStallDish, hiddenHallDish}) {
            var response = browser.get(path(id) + "?status=active&includeInactive=true");
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("DISH_NOT_FOUND");
        }
    }

    @Test
    void reflectsNameDescriptionPriceAndStatusChanges() throws Exception {
        UUID hall = hall("active");
        UUID stall = stall(hall, "active");
        UUID dish = dish(stall, "active", "0.00");
        var browser = login("user");
        assertThat(browser.get(path(dish)).statusCode()).isEqualTo(200);
        jdbc.update("update foodtime.dishes set name='新菜名',description='新简介',price=12.30 where id=?", dish);
        var data = json.readTree(browser.get(path(dish)).body()).path("data");
        assertThat(data.path("name").asString()).isEqualTo("新菜名");
        assertThat(data.path("description").asString()).isEqualTo("新简介");
        assertThat(new BigDecimal(data.path("price").asString())).isEqualByComparingTo("12.30");
        jdbc.update("update foodtime.dining_halls set status='inactive' where id=?", hall);
        assertThat(browser.get(path(dish)).statusCode()).isEqualTo(404);
    }

    @Test
    void validatesAuthenticationAndMalformedUuid() throws Exception {
        assertThat(new Browser().get(path(UUID.randomUUID())).statusCode()).isEqualTo(401);
        var browser = login("user");
        assertThat(browser.get("/api/v1/dishes/not-a-uuid").statusCode()).isEqualTo(400);
        jdbc.update("update foodtime.users set status='disabled' where id=?", userIds.get(0));
        assertThat(browser.get(path(UUID.randomUUID())).statusCode()).isEqualTo(401);
    }

    @Test
    void openApiDefinesNumericPriceNullableDescriptionAndSessionSecurity() throws Exception {
        var document = json.readTree(new Browser().get("/v3/api-docs").body());
        var get = document.path("paths").path("/api/v1/dishes/{dishId}").path("get");
        assertThat(get.path("security").get(0).has("sessionCookie")).isTrue();
        var properties = document.path("components").path("schemas").path("DishDetailResponse").path("properties");
        assertThat(properties.size()).isEqualTo(8);
        assertThat(properties.path("price").path("type").asString()).isEqualTo("number");
        var description = properties.path("description");
        assertThat(description.path("nullable").asBoolean() || description.path("type").toString().contains("\"null\"")).isTrue();
    }
}
