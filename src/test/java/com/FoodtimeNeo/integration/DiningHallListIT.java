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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rating-summary.refresh-enabled=false")
class DiningHallListIT {
    private static final String PREFIX = "foodtime:integration:halls:" + UUID.randomUUID();
    @Value("${local.server.port}") private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private PasswordEncoder passwords;
    private final JsonMapper json = JsonMapper.builder().build();
    private final List<UUID> hallIds = new ArrayList<>();
    private final List<UUID> userIds = new ArrayList<>();

    @DynamicPropertySource
    static void namespaces(DynamicPropertyRegistry properties) {
        properties.add("spring.session.redis.namespace", () -> PREFIX + ":sessions");
        properties.add("app.auth.redis-namespace", () -> PREFIX + ":login");
    }
    @AfterEach
    void cleanOnlyTestFixtures() {
        for (UUID id : hallIds) { jdbc.update("delete from foodtime.dining_halls where id = ?", id); }
        for (UUID id : userIds) { jdbc.update("delete from foodtime.users where id = ?", id); }
        Set<String> keys = redis.keys(PREFIX + ":*");
        if (keys != null && !keys.isEmpty()) { redis.delete(keys); }
    }

    @Test
    void anonymousRequestRequiresLogin() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/api/v1/dining-halls"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void returnsOnlyActiveHallsWithStableOrderingAndNullablePublicFields() throws Exception {
        UUID later = hall("active", 20, false);
        UUID tieOne = hall("active", 10, true);
        UUID tieTwo = hall("active", 10, false);
        UUID hidden = hall("inactive", -100, false);
        List<UUID> ties = new ArrayList<>(List.of(tieOne, tieTwo));
        ties.sort(Comparator.comparing(UUID::toString));
        Browser browser = login("user");
        var response = browser.get("/api/v1/dining-halls");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode document = json.readTree(response.body());
        assertThat(document.path("code").asString()).isEqualTo("OK");
        assertThat(response.headers().firstValue("X-Request-Id")).isNotEmpty();
        var data = document.path("data");
        assertThat(data.isArray()).isTrue();
        List<UUID> ownResults = new ArrayList<>();
        for (JsonNode item : data) {
            UUID id = UUID.fromString(item.path("id").asString());
            assertThat(id).isNotEqualTo(hidden);
            if (hallIds.contains(id)) { ownResults.add(id); }
            assertThat(item.size()).isEqualTo(6);
            assertThat(item.has("status")).isFalse();
            assertThat(item.has("sortOrder")).isFalse();
            assertThat(item.has("createdAt")).isFalse();
            if (id.equals(tieOne)) {
                assertThat(item.path("latitude").asDouble()).isEqualTo(39.952);
                assertThat(item.path("longitude").asDouble()).isEqualTo(116.35);
                assertThat(item.path("coverImageUrl").asString()).isEqualTo("https://example.test/hall.jpg");
            }
            if (id.equals(later)) {
                for (String field : List.of("description", "coverImageUrl", "latitude", "longitude")) {
                    assertThat(item.has(field)).isTrue();
                    assertThat(item.path(field).isNull()).isTrue();
                }
            }
        }
        assertThat(ownResults).containsExactly(ties.get(0), ties.get(1), later);
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "admin", "superadmin"})
    void listDisplayRulesApplyToEveryRoleAndCannotBeOverriddenByQueryParameters(String role) throws Exception {
        UUID active = hall("active", 0, false);
        UUID hidden = hall("inactive", 0, false);
        var response = login(role).get("/api/v1/dining-halls?status=inactive&sortOrder=desc");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(active.toString()).doesNotContain(hidden.toString());
    }

    @Test
    void subsequentRequestsReflectDisplayStatusChanges() throws Exception {
        UUID id = hall("active", 0, false);
        Browser browser = login("user");
        assertThat(browser.get("/api/v1/dining-halls").body()).contains(id.toString());
        jdbc.update("update foodtime.dining_halls set status = 'inactive' where id = ?", id);
        assertThat(browser.get("/api/v1/dining-halls").body()).doesNotContain(id.toString());
        jdbc.update("update foodtime.dining_halls set status = 'active' where id = ?", id);
        assertThat(browser.get("/api/v1/dining-halls").body()).contains(id.toString());
    }

    @Test
    void disabledUserCannotReadUsingExistingCookie() throws Exception {
        Browser browser = login("user");
        jdbc.update("update foodtime.users set status = 'disabled' where id = ?", userIds.get(0));
        var response = browser.get("/api/v1/dining-halls");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void openApiDocumentsAuthenticatedArrayOfDiningHallResponses() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode document = json.readTree(response.body());
        var get = document.path("paths").path("/api/v1/dining-halls").path("get");
        assertThat(get.path("security").get(0).has("sessionCookie")).isTrue();
        assertThat(document.path("components").path("schemas").has("DiningHallResponse")).isTrue();
        var properties = document.path("components").path("schemas").path("DiningHallResponse").path("properties");
        assertThat(properties.has("latitude")).isTrue();
        assertThat(properties.has("sortOrder")).isFalse();
        for (String field : List.of("coverImageUrl", "description", "latitude", "longitude")) {
            var property = properties.path(field);
            assertThat(property.path("nullable").asBoolean() || property.path("type").toString().contains("\"null\""))
                    .as("OpenAPI allows null for %s", field).isTrue();
        }
    }

    private UUID hall(String status, int order, boolean metadata) {
        UUID id = UUID.randomUUID();
        hallIds.add(id);
        jdbc.update("""
                insert into foodtime.dining_halls(id,name,status,sort_order,cover_image_url,description,latitude,longitude)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "食堂列表测试-" + id, status, order,
                metadata ? "https://example.test/hall.jpg" : null, metadata ? "测试食堂简介" : null,
                metadata ? new BigDecimal("39.9520000") : null, metadata ? new BigDecimal("116.3500000") : null);
        return id;
    }
    private Browser login(String role) throws Exception {
        UUID id = UUID.randomUUID();
        userIds.add(id);
        String email = Long.toUnsignedString(ThreadLocalRandom.current().nextLong()) + "@bjtu.edu.cn";
        jdbc.update("""
                insert into foodtime.users(id,email,password_hash,display_name,role,email_verified_at)
                values (?, ?, ?, '干饭人123456', ?, statement_timestamp())
                """, id, email, passwords.encode("Hall12345!"), role);
        Browser browser = new Browser();
        String token = json.readTree(browser.get("/api/v1/auth/csrf").body()).path("data").path("token").asString();
        var response = browser.client.send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("account", email, "password", "Hall12345!")),
                        StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return browser;
    }
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
