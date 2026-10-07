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
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rating-summary.refresh-enabled=false")
class StallListIT {
    private static final String PREFIX = "foodtime:integration:stalls:" + UUID.randomUUID();
    @Value("${local.server.port}") private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private PasswordEncoder passwords;
    private final JsonMapper json = JsonMapper.builder().build();
    private final List<UUID> hallIds = new ArrayList<>();
    private final List<UUID> stallIds = new ArrayList<>();
    private final List<UUID> userIds = new ArrayList<>();

    @DynamicPropertySource
    static void namespaces(DynamicPropertyRegistry properties) {
        properties.add("spring.session.redis.namespace", () -> PREFIX + ":sessions");
        properties.add("app.auth.redis-namespace", () -> PREFIX + ":login");
    }
    @AfterEach
    void cleanOnlyTestFixtures() {
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
    void scopesToParentFiltersInactiveStallsAndPreservesStableOrderAndNulls() throws Exception {
        UUID parent = hall("active");
        UUID other = hall("active");
        UUID later = stall(parent, "active", 20, false);
        UUID one = stall(parent, "active", 10, true);
        UUID two = stall(parent, "active", 10, false);
        UUID hidden = stall(parent, "inactive", -100, false);
        UUID elsewhere = stall(other, "active", -200, true);
        var response = login("user").get(path(parent));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain(hidden.toString(), elsewhere.toString());
        assertThat(response.headers().firstValue("X-Request-Id")).isNotEmpty();
        JsonNode data = json.readTree(response.body()).path("data");
        assertThat(data.size()).isEqualTo(3);
        List<UUID> ids = new ArrayList<>();
        for (JsonNode item : data) {
            UUID id = UUID.fromString(item.path("id").asString());
            ids.add(id);
            assertThat(item.path("diningHallId").asString()).isEqualTo(parent.toString());
            assertThat(item.size()).isEqualTo(6);
            assertThat(item.has("status")).isFalse();
            assertThat(item.has("sortOrder")).isFalse();
            if (id.equals(one)) {
                assertThat(item.path("floor").asString()).isEqualTo("一层");
                assertThat(item.path("description").asString()).isEqualTo("测试档口简介");
                assertThat(item.path("coverImageUrl").asString()).isEqualTo("https://example.test/stall.jpg");
            }
            if (id.equals(later)) {
                for (String field : List.of("floor", "description", "coverImageUrl")) {
                    assertThat(item.has(field)).isTrue();
                    assertThat(item.path(field).isNull()).isTrue();
                }
            }
        }
        var ties = new ArrayList<>(List.of(one, two));
        ties.sort(Comparator.comparing(UUID::toString));
        assertThat(ids).containsExactly(ties.get(0), ties.get(1), later);
    }

    @Test
    void missingAndInactiveHallsReturn404EvenWithActiveChildren() throws Exception {
        UUID hidden = hall("inactive");
        UUID child = stall(hidden, "active", 0, true);
        Browser browser = login("user");
        for (UUID parent : List.of(UUID.randomUUID(), hidden)) {
            var response = browser.get(path(parent));
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("DINING_HALL_NOT_FOUND");
            assertThat(response.body()).doesNotContain(child.toString());
        }
    }

    @Test
    void activeHallWithoutStallsOrWithOnlyInactiveStallsReturnsEmptyArray() throws Exception {
        UUID parent = hall("active");
        Browser browser = login("user");
        var empty = browser.get(path(parent));
        assertThat(empty.statusCode()).isEqualTo(200);
        assertThat(json.readTree(empty.body()).path("data").isArray()).isTrue();
        assertThat(json.readTree(empty.body()).path("data").size()).isZero();
        stall(parent, "inactive", 0, true);
        var hiddenOnly = browser.get(path(parent));
        assertThat(hiddenOnly.statusCode()).isEqualTo(200);
        assertThat(json.readTree(hiddenOnly.body()).path("data").size()).isZero();
    }

    @Test
    void subsequentRequestsReflectBothParentAndStallStatusChanges() throws Exception {
        UUID parent = hall("active");
        UUID child = stall(parent, "active", 0, true);
        Browser browser = login("user");
        assertThat(browser.get(path(parent)).body()).contains(child.toString());
        jdbc.update("update foodtime.stalls set status = 'inactive' where id = ?", child);
        assertThat(json.readTree(browser.get(path(parent)).body()).path("data").size()).isZero();
        jdbc.update("update foodtime.stalls set status = 'active' where id = ?", child);
        jdbc.update("update foodtime.dining_halls set status = 'inactive' where id = ?", parent);
        assertThat(browser.get(path(parent)).statusCode()).isEqualTo(404);
        jdbc.update("update foodtime.dining_halls set status = 'active' where id = ?", parent);
        assertThat(browser.get(path(parent)).body()).contains(child.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin", "superadmin"})
    void queryParametersAndRolesCannotOverrideParentOrDisplayStatus(String role) throws Exception {
        UUID parent = hall("active");
        UUID other = hall("active");
        UUID active = stall(parent, "active", 0, false);
        UUID hidden = stall(parent, "inactive", 0, false);
        UUID elsewhere = stall(other, "active", 0, false);
        var response = login(role).get(path(parent) + "?status=inactive&diningHallId=" + other + "&sortOrder=desc");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(active.toString()).doesNotContain(hidden.toString(), elsewhere.toString());
    }

    @Test
    void malformedUuidReturns400ForAuthenticatedUser() throws Exception {
        var response = login("user").get("/api/v1/dining-halls/not-a-uuid/stalls");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void disabledUserCannotReadWithExistingSession() throws Exception {
        UUID parent = hall("active");
        Browser browser = login("user");
        jdbc.update("update foodtime.users set status = 'disabled' where id = ?", userIds.get(0));
        assertThat(browser.get(path(parent)).statusCode()).isEqualTo(401);
    }

    @Test
    void openApiIncludesPathIdSessionCookieAndNullableDisplayFields() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var document = json.readTree(response.body());
        var get = document.path("paths").path("/api/v1/dining-halls/{diningHallId}/stalls").path("get");
        assertThat(get.path("security").get(0).has("sessionCookie")).isTrue();
        assertThat(get.path("parameters").get(0).path("schema").path("format").asString()).isEqualTo("uuid");
        var properties = document.path("components").path("schemas").path("StallResponse").path("properties");
        assertThat(properties.has("diningHallId")).isTrue();
        assertThat(properties.has("status")).isFalse();
        assertThat(properties.has("sortOrder")).isFalse();
        for (String field : List.of("coverImageUrl", "description", "floor")) {
            var property = properties.path(field);
            assertThat(property.path("nullable").asBoolean() || property.path("type").toString().contains("\"null\""))
                    .as("OpenAPI allows null for %s", field).isTrue();
        }
    }

    private UUID hall(String status) {
        UUID id = UUID.randomUUID();
        hallIds.add(id);
        jdbc.update("insert into foodtime.dining_halls(id,name,status) values (?, ?, ?)", id, "档口列表测试食堂-" + id, status);
        return id;
    }
    private UUID stall(UUID hall, String status, int sort, boolean metadata) {
        UUID id = UUID.randomUUID();
        stallIds.add(id);
        jdbc.update("""
                insert into foodtime.stalls(id,dining_hall_id,name,status,sort_order,description,cover_image_url,floor)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, hall, "档口测试-" + id, status, sort, metadata ? "测试档口简介" : null,
                metadata ? "https://example.test/stall.jpg" : null, metadata ? "一层" : null);
        return id;
    }
    private Browser login(String role) throws Exception {
        UUID id = UUID.randomUUID();
        userIds.add(id);
        String email = Long.toUnsignedString(ThreadLocalRandom.current().nextLong()) + "@bjtu.edu.cn";
        jdbc.update("""
                insert into foodtime.users(id,email,password_hash,display_name,role,email_verified_at)
                values (?, ?, ?, '干饭人123456', ?, statement_timestamp())
                """, id, email, passwords.encode("Stall12345!"), role);
        Browser browser = new Browser();
        String token = json.readTree(browser.get("/api/v1/auth/csrf").body()).path("data").path("token").asString();
        var response = browser.client.send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("account", email, "password", "Stall12345!")),
                        StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return browser;
    }
    private String path(UUID hall) { return "/api/v1/dining-halls/" + hall + "/stalls"; }
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
