package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class ReadApiITSupport {
    private static final String PREFIX = "foodtime:integration:read-api:" + UUID.randomUUID();
    @Value("${local.server.port}") private int port;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private PasswordEncoder passwords;
    protected final JsonMapper json = JsonMapper.builder().build();
    protected final List<UUID> hallIds = new ArrayList<>();
    protected final List<UUID> stallIds = new ArrayList<>();
    protected final List<UUID> dishIds = new ArrayList<>();
    protected final List<UUID> userIds = new ArrayList<>();

    @DynamicPropertySource
    static void namespaces(DynamicPropertyRegistry properties) {
        properties.add("spring.session.redis.namespace", () -> PREFIX + ":sessions");
        properties.add("app.auth.redis-namespace", () -> PREFIX + ":login");
    }

    @AfterEach
    void cleanReadFixtures() {
        cleanContentFixtures();
        for (UUID id : dishIds) { jdbc.update("delete from foodtime.dishes where id = ?", id); }
        for (UUID id : stallIds) { jdbc.update("delete from foodtime.stalls where id = ?", id); }
        for (UUID id : hallIds) { jdbc.update("delete from foodtime.dining_halls where id = ?", id); }
        for (UUID id : userIds) { jdbc.update("delete from foodtime.users where id = ?", id); }
        Set<String> keys = redis.keys(PREFIX + ":*");
        if (keys != null && !keys.isEmpty()) { redis.delete(keys); }
    }
    protected void cleanContentFixtures() { }

    protected UUID hall(String status) {
        UUID id = UUID.randomUUID();
        hallIds.add(id);
        jdbc.update("insert into foodtime.dining_halls(id,name,status) values (?, ?, ?)", id, "查询测试食堂-" + id, status);
        return id;
    }
    protected UUID stall(UUID hall, String status) {
        UUID id = UUID.randomUUID();
        stallIds.add(id);
        jdbc.update("insert into foodtime.stalls(id,dining_hall_id,name,status) values (?, ?, ?, ?)", id, hall, "查询测试档口-" + id, status);
        return id;
    }
    protected UUID dish(UUID stall, String status, String price) {
        UUID id = UUID.randomUUID();
        dishIds.add(id);
        jdbc.update("insert into foodtime.dishes(id,stall_id,name,status,price) values (?, ?, ?, ?, ?)",
                id, stall, "查询测试菜品-" + id, status, new BigDecimal(price));
        return id;
    }
    protected UUID user(String role) {
        UUID id = UUID.randomUUID();
        userIds.add(id);
        String email = Long.toUnsignedString(ThreadLocalRandom.current().nextLong()) + "@bjtu.edu.cn";
        jdbc.update("""
                insert into foodtime.users(id,email,password_hash,display_name,role,email_verified_at)
                values (?, ?, ?, '干饭人123456', ?, statement_timestamp())
                """, id, email, passwords.encode("Read12345!"), role);
        return id;
    }
    protected Browser login(String role) throws Exception { return login(user(role)); }
    protected Browser login(UUID userId) throws Exception {
        String email = jdbc.queryForObject("select email from foodtime.users where id = ?", String.class, userId);
        Browser browser = new Browser();
        String token = json.readTree(browser.get("/api/v1/auth/csrf").body()).path("data").path("token").asString();
        var response = browser.client.send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("account", email, "password", "Read12345!")),
                        StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return browser;
    }
    protected URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    protected class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).build();
        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
    }
}
