package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Real HTTP/cookies/CSRF, with independently owned users and catalog fixtures. */
abstract class IntegrationApiSupport {
    private static final String PREFIX = "foodtime:integration:api:" + UUID.randomUUID();
    static final String PASSWORD = "Foodtime2026!";
    @Value("${local.server.port}") protected int port;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PasswordEncoder passwords;
    @Autowired private StringRedisTemplate redis;
    protected final JsonMapper json = JsonMapper.builder().build();
    protected final List<UUID> users = new ArrayList<>();
    protected final List<UUID> additionalHalls = new ArrayList<>();
    protected UUID hall, stall, dish;
    protected Account owner, other;
    protected record Account(UUID id, String email) { }

    @DynamicPropertySource
    static void namespaces(DynamicPropertyRegistry properties) {
        properties.add("spring.session.redis.namespace", () -> PREFIX + ":sessions");
        properties.add("app.auth.redis-namespace", () -> PREFIX + ":limits");
    }

    @BeforeEach
    void fixtures() {
        owner = account("user"); other = account("user");
        hall = UUID.randomUUID(); stall = UUID.randomUUID(); dish = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dining_halls(id,name) VALUES (?, 'integration hall')", hall);
        jdbc.update("INSERT INTO foodtime.stalls(id,dining_hall_id,name) VALUES (?, ?, 'integration stall')", stall, hall);
        jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,price) VALUES (?, ?, 'integration dish', 12.30)", dish, stall);
    }

    protected Account account(String role) {
        UUID id = UUID.randomUUID(); users.add(id);
        String email = Long.toUnsignedString(id.getMostSignificantBits()) + "@bjtu.edu.cn";
        jdbc.update("""
                INSERT INTO foodtime.users(id,email,password_hash,display_name,role,email_verified_at)
                VALUES (?, ?, ?, 'integration user', ?, now())
                """, id, email, passwords.encode(PASSWORD), role);
        return new Account(id, email);
    }

    @AfterEach
    void cleanFixtures() {
        for (UUID user : users) {
            jdbc.update("DELETE FROM foodtime.content_moderation_logs WHERE moderator_user_id = ?", user);
        }
        for (UUID user : users) {
            jdbc.update("DELETE FROM foodtime.dish_reviews WHERE user_id = ?", user);
            jdbc.update("DELETE FROM foodtime.dish_review_submissions WHERE user_id = ?", user);
            jdbc.update("DELETE FROM foodtime.dish_submissions WHERE submitted_by_user_id = ?", user);
            jdbc.update("DELETE FROM foodtime.stall_submissions WHERE submitted_by_user_id = ?", user);
            jdbc.update("DELETE FROM foodtime.dish_images WHERE uploaded_by_user_id = ?", user);
        }
        var ownedHalls = new ArrayList<>(additionalHalls); ownedHalls.add(hall);
        for (UUID hallId : ownedHalls) {
            jdbc.update("DELETE FROM foodtime.dishes WHERE stall_id IN (SELECT id FROM foodtime.stalls WHERE dining_hall_id = ?)", hallId);
            jdbc.update("DELETE FROM foodtime.stalls WHERE dining_hall_id = ?", hallId);
            jdbc.update("DELETE FROM foodtime.dining_halls WHERE id = ?", hallId);
        }
        for (UUID user : users) { jdbc.update("DELETE FROM foodtime.users WHERE id = ?", user); }
        Set<String> keys = redis.keys(PREFIX + ":*");
        if (keys != null && !keys.isEmpty()) { redis.delete(keys); }
    }

    protected Browser login(Account account) throws Exception {
        Browser browser = new Browser(); browser.csrf();
        assertThat(browser.call("POST", "/api/v1/auth/login", Map.of("account", account.email(), "password", PASSWORD)).statusCode()).isEqualTo(200);
        browser.csrf(); return browser;
    }

    protected class Browser {
        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        private String token;
        void csrf() throws Exception {
            token = body(call("GET", "/api/v1/auth/csrf", null)).path("data").path("token").asString();
        }
        HttpResponse<String> call(String method, String path, Object data) throws Exception {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
            if (data != null) { request.header("Content-Type", "application/json"); }
            if (token != null) { request.header("X-CSRF-TOKEN", token); }
            request.method(method, data == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data)));
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> upload(byte[] image) throws Exception {
            String boundary = "foodtime" + UUID.randomUUID();
            var payload = new java.io.ByteArrayOutputStream();
            payload.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            payload.write(image);
            payload.write(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/uploads/images"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary);
            if (token != null) request.header("X-CSRF-TOKEN", token);
            return client.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(payload.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<byte[]> image(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        }
    }

    protected JsonNode body(HttpResponse<String> response) { return json.readTree(response.body()); }
    protected String dishPath(String suffix) { return "/api/v1/dishes/" + dish + suffix; }
    protected UUID formal(Account account, int rating) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dish_reviews(id,dish_id,user_id,rating,content) VALUES (?, ?, ?, ?, 'published')",
                id, dish, account.id(), rating);
        return id;
    }
}
