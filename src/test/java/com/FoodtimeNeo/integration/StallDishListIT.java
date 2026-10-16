package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rating-summary.refresh-enabled=false")
class StallDishListIT {
    private static final String PREFIX = "foodtime:integration:stall-dishes:" + UUID.randomUUID();
    @Value("${local.server.port}") private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private PasswordEncoder passwords;
    private final JsonMapper json = JsonMapper.builder().build();
    private final List<UUID> hallIds = new ArrayList<>();
    private final List<UUID> stallIds = new ArrayList<>();
    private final List<UUID> dishIds = new ArrayList<>();
    private final List<UUID> userIds = new ArrayList<>();

    @DynamicPropertySource
    static void namespaces(DynamicPropertyRegistry properties) {
        properties.add("spring.session.redis.namespace", () -> PREFIX + ":sessions");
        properties.add("app.auth.redis-namespace", () -> PREFIX + ":login");
    }

    @AfterEach
    void cleanOnlyTestFixtures() {
        for (UUID id : dishIds) { jdbc.update("delete from foodtime.dishes where id = ?", id); }
        for (UUID id : stallIds) { jdbc.update("delete from foodtime.stalls where id = ?", id); }
        for (UUID id : hallIds) { jdbc.update("delete from foodtime.dining_halls where id = ?", id); }
        for (UUID id : userIds) { jdbc.update("delete from foodtime.users where id = ?", id); }
        Set<String> keys = redis.keys(PREFIX + ":*");
        if (keys != null && !keys.isEmpty()) { redis.delete(keys); }
    }

    @Test
    void anonymousRequestRequiresLogin() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri(path(UUID.randomUUID())))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void scopesToStallFiltersInactiveDishesAndOrdersByCreationTimeThenId() throws Exception {
        UUID hall = hall("active");
        UUID parent = stall(hall, "active");
        UUID other = stall(hall, "active");
        UUID later = dish(parent, "active", "12.30", "2026-01-02T00:00:00Z");
        UUID one = dish(parent, "active", "0.00", "2026-01-01T00:00:00Z");
        UUID two = dish(parent, "active", "99999999.99", "2026-01-01T00:00:00Z");
        UUID hidden = dish(parent, "inactive", "1.00", "2025-01-01T00:00:00Z");
        UUID elsewhere = dish(other, "active", "2.00", "2025-01-01T00:00:00Z");
        var response = login("user").get(path(parent));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain(hidden.toString(), elsewhere.toString());
        assertThat(response.headers().firstValue("X-Request-Id")).isNotEmpty();
        JsonNode data = json.readTree(response.body()).path("data");
        assertThat(data.size()).isEqualTo(3);
        List<UUID> ids = new ArrayList<>();
        Map<UUID, BigDecimal> prices = Map.of(one, new BigDecimal("0.00"), two, new BigDecimal("99999999.99"), later, new BigDecimal("12.30"));
        for (JsonNode item : data) {
            UUID id = UUID.fromString(item.path("id").asString());
            ids.add(id);
            assertThat(item.path("stallId").asString()).isEqualTo(parent.toString());
            assertThat(item.path("name").asString()).isEqualTo("菜品测试-" + id);
            assertThat(item.size()).isEqualTo(4);
            assertThat(item.path("price").isNumber()).isTrue();
            assertThat(new BigDecimal(item.path("price").asString())).isEqualByComparingTo(prices.get(id));
        }
        var ties = new ArrayList<>(List.of(one, two));
        ties.sort(Comparator.comparing(UUID::toString));
        assertThat(ids).containsExactly(ties.get(0), ties.get(1), later);
    }

    @Test
    void missingInactiveStallsAndInactiveHallsReturn404EvenWithActiveDishes() throws Exception {
        UUID hiddenStall = stall(hall("active"), "inactive");
        UUID hiddenHallStall = stall(hall("inactive"), "active");
        UUID child = dish(hiddenStall, "active", "10.00", "2026-01-01T00:00:00Z");
        UUID otherChild = dish(hiddenHallStall, "active", "10.00", "2026-01-01T00:00:00Z");
        Browser browser = login("user");
        for (UUID parent : List.of(UUID.randomUUID(), hiddenStall, hiddenHallStall)) {
            var response = browser.get(path(parent));
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("STALL_NOT_FOUND");
            assertThat(response.body()).doesNotContain(child.toString(), otherChild.toString());
        }
    }

    @Test
    void visibleStallWithoutDishesOrWithOnlyInactiveDishesReturnsEmptyArray() throws Exception {
        UUID parent = stall(hall("active"), "active");
        Browser browser = login("user");
        var empty = browser.get(path(parent));
        assertThat(empty.statusCode()).isEqualTo(200);
        assertThat(json.readTree(empty.body()).path("data").isArray()).isTrue();
        assertThat(json.readTree(empty.body()).path("data").size()).isZero();
        dish(parent, "inactive", "1.00", "2026-01-01T00:00:00Z");
        var hiddenOnly = browser.get(path(parent));
        assertThat(hiddenOnly.statusCode()).isEqualTo(200);
        assertThat(json.readTree(hiddenOnly.body()).path("data").size()).isZero();
    }

    @Test
    void subsequentRequestsReflectDishMetadataAndAllThreeLevelsOfStatusChanges() throws Exception {
        UUID hall = hall("active");
        UUID parent = stall(hall, "active");
        UUID child = dish(parent, "active", "10.00", "2026-01-01T00:00:00Z");
        Browser browser = login("user");
        assertThat(browser.get(path(parent)).body()).contains(child.toString());
        jdbc.update("update foodtime.dishes set name = '改名菜品', price = 13.25 where id = ?", child);
        var updated = json.readTree(browser.get(path(parent)).body()).path("data").get(0);
        assertThat(updated.path("name").asString()).isEqualTo("改名菜品");
        assertThat(new BigDecimal(updated.path("price").asString())).isEqualByComparingTo("13.25");
        jdbc.update("update foodtime.dishes set status = 'inactive' where id = ?", child);
        assertThat(json.readTree(browser.get(path(parent)).body()).path("data").size()).isZero();
        jdbc.update("update foodtime.dishes set status = 'active' where id = ?", child);
        jdbc.update("update foodtime.stalls set status = 'inactive' where id = ?", parent);
        assertThat(browser.get(path(parent)).statusCode()).isEqualTo(404);
        jdbc.update("update foodtime.stalls set status = 'active' where id = ?", parent);
        jdbc.update("update foodtime.dining_halls set status = 'inactive' where id = ?", hall);
        assertThat(browser.get(path(parent)).statusCode()).isEqualTo(404);
        jdbc.update("update foodtime.dining_halls set status = 'active' where id = ?", hall);
        assertThat(browser.get(path(parent)).body()).contains(child.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin", "superadmin"})
    void queryParametersAndRolesCannotOverrideStallOrDisplayStatus(String role) throws Exception {
        UUID hall = hall("active");
        UUID parent = stall(hall, "active");
        UUID other = stall(hall, "active");
        UUID active = dish(parent, "active", "1.00", "2026-01-01T00:00:00Z");
        UUID hidden = dish(parent, "inactive", "1.00", "2026-01-01T00:00:00Z");
        UUID elsewhere = dish(other, "active", "1.00", "2026-01-01T00:00:00Z");
        var response = login(role).get(path(parent) + "?status=inactive&stallId=" + other + "&sort=desc");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(active.toString()).doesNotContain(hidden.toString(), elsewhere.toString());
    }

    @Test
    void malformedUuidReturns400ForAuthenticatedUser() throws Exception {
        var response = login("user").get("/api/v1/stalls/not-a-uuid/dishes");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void disabledUserCannotReadWithExistingSession() throws Exception {
        UUID parent = stall(hall("active"), "active");
        Browser browser = login("user");
        jdbc.update("update foodtime.users set status = 'disabled' where id = ?", userIds.get(0));
        assertThat(browser.get(path(parent)).statusCode()).isEqualTo(401);
    }

    @Test
    void openApiIncludesPathIdSessionCookieAndNumericPrice() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var document = json.readTree(response.body());
        var get = document.path("paths").path("/api/v1/stalls/{stallId}/dishes").path("get");
        assertThat(get.path("security").get(0).has("sessionCookie")).isTrue();
        assertThat(get.path("parameters").get(0).path("schema").path("format").asString()).isEqualTo("uuid");
        var properties = document.path("components").path("schemas").path("DishListItemResponse").path("properties");
        assertThat(properties.size()).isEqualTo(4);
        assertThat(properties.has("stallId")).isTrue();
        assertThat(properties.path("price").path("type").asString()).isEqualTo("number");
    }

    private UUID hall(String status) {
        UUID id = UUID.randomUUID();
        hallIds.add(id);
        jdbc.update("insert into foodtime.dining_halls(id,name,status) values (?, ?, ?)", id, "菜品列表测试食堂-" + id, status);
        return id;
    }
    private UUID stall(UUID hall, String status) {
        UUID id = UUID.randomUUID();
        stallIds.add(id);
        jdbc.update("insert into foodtime.stalls(id,dining_hall_id,name,status) values (?, ?, ?, ?)", id, hall, "菜品列表测试档口-" + id, status);
        return id;
    }
    private UUID dish(UUID stall, String status, String price, String createdAt) {
        UUID id = UUID.randomUUID();
        dishIds.add(id);
        jdbc.update("insert into foodtime.dishes(id,stall_id,name,status,price,created_at) values (?, ?, ?, ?, ?, ?)",
                id, stall, "菜品测试-" + id, status, new BigDecimal(price), java.sql.Timestamp.from(Instant.parse(createdAt)));
        return id;
    }
    private Browser login(String role) throws Exception {
        UUID id = UUID.randomUUID();
        userIds.add(id);
        String email = Long.toUnsignedString(ThreadLocalRandom.current().nextLong()) + "@bjtu.edu.cn";
        jdbc.update("""
                insert into foodtime.users(id,email,password_hash,display_name,role,email_verified_at)
                values (?, ?, ?, '干饭人123456', ?, statement_timestamp())
                """, id, email, passwords.encode("Dish12345!"), role);
        Browser browser = new Browser();
        String token = json.readTree(browser.get("/api/v1/auth/csrf").body()).path("data").path("token").asString();
        var response = browser.client.send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("account", email, "password", "Dish12345!")),
                        StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return browser;
    }
    private String path(UUID stall) { return "/api/v1/stalls/" + stall + "/dishes"; }
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    private class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).build();
        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
    }
}
