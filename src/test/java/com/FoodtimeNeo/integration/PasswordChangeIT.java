package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.auth.service.PasswordChangeRateLimiter;
import com.FoodtimeNeo.config.SecurityConfig;
import com.FoodtimeNeo.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rating-summary.refresh-enabled=false", "app.auth.password-change.user-limit=3",
                "app.auth.password-change.ip-limit=6"})
class PasswordChangeIT {
    private static final String PREFIX = "foodtime:integration:password:" + UUID.randomUUID();
    private static final String OLD = "Old12345!";
    private static final String NEW = "New12345!";
    @Value("${local.server.port}") private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private PasswordEncoder passwords;
    @Autowired private UserMapper users;
    @Autowired private PasswordChangeRateLimiter limiter;
    private final JsonMapper json = JsonMapper.builder().build();
    private final List<String> emails = new ArrayList<>();

    @DynamicPropertySource
    static void namespaces(DynamicPropertyRegistry properties) {
        properties.add("spring.session.redis.namespace", () -> PREFIX + ":sessions");
        properties.add("app.auth.redis-namespace", () -> PREFIX + ":login");
        properties.add("app.auth.password-change.redis-namespace", () -> PREFIX + ":changes");
    }
    @AfterEach
    void cleanOnlyTestData() {
        for (String email : emails) { jdbc.update("delete from foodtime.users where email = ?", email); }
        Set<String> keys = redis.keys(PREFIX + ":*");
        if (keys != null && !keys.isEmpty()) { redis.delete(keys); }
    }

    @Test
    void changeInvalidatesCurrentAndOtherSessionsAndOnlyNewPasswordCanLogin() throws Exception {
        String email = account();
        var original = users.findLoginAccount(email);
        Browser first = loggedIn(email);
        Browser second = loggedIn(email);
        String replayCookie = first.cookie();
        var changed = first.change(OLD, NEW);
        assertThat(changed.statusCode()).isEqualTo(200);
        assertThat(json.readTree(changed.body()).path("code").asString()).isEqualTo("OK");
        assertThat(changed.body()).doesNotContain(OLD, NEW, "argon2", "passwordHash");
        assertThat(changed.headers().allValues("Set-Cookie")).anySatisfy(cookie ->
                assertThat(cookie).contains(SecurityConfig.SESSION_COOKIE + "=", "Max-Age=0", "HttpOnly"));
        var updated = users.findLoginAccount(email);
        assertThat(updated.passwordHash()).startsWith("{argon2id}$argon2id$").isNotEqualTo(original.passwordHash());
        assertThat(passwords.matches(NEW, updated.passwordHash())).isTrue();
        assertThat(passwords.matches(OLD, updated.passwordHash())).isFalse();
        assertThat(updated.passwordChangedAt()).isAfter(original.passwordChangedAt());
        assertThat(first.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(second.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/api/v1/auth/me"))
                .header("Cookie", replayCookie).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        Browser retry = new Browser();
        retry.csrf();
        assertThat(retry.login(email, OLD).statusCode()).isEqualTo(401);
        assertThat(retry.login(email, NEW).statusCode()).isEqualTo(200);
        assertThat(retry.get("/api/v1/auth/me").statusCode()).isEqualTo(200);
    }

    @Test
    void wrongOldAndUnchangedPasswordsPreserveHashAndSession() throws Exception {
        String email = account();
        var before = users.findLoginAccount(email);
        Browser browser = loggedIn(email);
        var wrong = browser.change("Wrong123!", NEW);
        assertThat(wrong.statusCode()).isEqualTo(400);
        assertThat(json.readTree(wrong.body()).path("code").asString()).isEqualTo("OLD_PASSWORD_INCORRECT");
        var unchanged = browser.change(OLD, OLD);
        assertThat(unchanged.statusCode()).isEqualTo(400);
        assertThat(json.readTree(unchanged.body()).path("code").asString()).isEqualTo("PASSWORD_UNCHANGED");
        assertThat(users.findLoginAccount(email)).isEqualTo(before);
        assertThat(browser.get("/api/v1/auth/me").statusCode()).isEqualTo(200);
    }

    @Test
    void csrfAndAuthenticationAreRequiredAndInvalidInputDoesNotConsumeQuota() throws Exception {
        String email = account();
        Browser anonymous = new Browser();
        anonymous.csrf();
        assertThat(anonymous.change(OLD, NEW).statusCode()).isEqualTo(401);
        Browser browser = loggedIn(email);
        assertThat(browser.send("PUT", "/api/v1/users/me/password", body(OLD, NEW), false).statusCode()).isEqualTo(403);
        assertThat(browser.change(OLD, "abcdefgh").statusCode()).isEqualTo(400);
        assertThat(redis.hasKey(limiter.key("user", users.findLoginAccount(email).id().toString()))).isFalse();
        assertThat(passwords.matches(OLD, users.findLoginAccount(email).passwordHash())).isTrue();
    }

    @Test
    void clientCannotSelectAnotherAccountOrChangeRole() throws Exception {
        String owner = account();
        String victim = account();
        var victimBefore = users.findLoginAccount(victim);
        Browser browser = loggedIn(owner);
        var response = browser.send("PUT", "/api/v1/users/me/password", json.writeValueAsString(Map.of(
                "oldPassword", OLD, "newPassword", NEW, "userId", victimBefore.id(), "email", victim, "role", "superadmin")), true);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(users.findLoginAccount(victim)).isEqualTo(victimBefore);
        assertThat(users.findLoginAccount(owner).role()).isEqualTo("user");
        assertThat(passwords.matches(NEW, users.findLoginAccount(owner).passwordHash())).isTrue();
    }

    @Test
    void concurrentChangesCannotOverwriteTheWinningPassword() throws Exception {
        String email = account();
        Browser first = loggedIn(email);
        Browser second = loggedIn(email);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> { start.await(); return first.change(OLD, NEW).statusCode(); });
            var two = executor.submit(() -> { start.await(); return second.change(OLD, "Other123!").statusCode(); });
            start.countDown();
            List<Integer> statuses = List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
            assertThat(statuses.stream().filter(status -> status == 200).count()).isEqualTo(1);
            assertThat(statuses).allMatch(status -> Set.of(200, 401, 403, 409).contains(status));
            String winner = statuses.get(0) == 200 ? NEW : "Other123!";
            assertThat(passwords.matches(winner, users.findLoginAccount(email).passwordHash())).isTrue();
        }
    }

    @Test
    void passwordVersionStrictlyIncreasesAndStaleWritesAreRejectedEvenAfterClockSkew() throws Exception {
        String email = account();
        jdbc.update("update foodtime.users set password_changed_at = clock_timestamp() + interval '1 day' where email = ?", email);
        var before = users.findLoginAccount(email);
        Browser browser = loggedIn(email);
        assertThat(browser.change(OLD, NEW).statusCode()).isEqualTo(200);
        assertThat(users.findLoginAccount(email).passwordChangedAt()).isAfter(before.passwordChangedAt());
        assertThat(users.changePassword(before.id(), before.passwordHash(), before.passwordChangedAt(), "must-not-be-stored")).isZero();
        assertThat(passwords.matches(NEW, users.findLoginAccount(email).passwordHash())).isTrue();
    }

    @Test
    void userLimitPersistsAcrossSessionsAndIsIndependentOfLoginLimit() throws Exception {
        String email = account();
        Browser browser = loggedIn(email);
        for (int i = 0; i < 3; i++) { assertThat(browser.change("Wrong123!", NEW).statusCode()).isEqualTo(400); }
        Browser another = loggedIn(email);
        var response = another.change(OLD, NEW);
        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("PASSWORD_CHANGE_RATE_LIMITED");
        assertThat(Long.parseLong(response.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L, 900L);
        assertThat(users.findLoginAccount(email).passwordHash()).doesNotContain(NEW);
        assertThat(passwords.matches(OLD, users.findLoginAccount(email).passwordHash())).isTrue();
    }

    @Test
    void ipLimitCannotBeBypassedByChangingAccountsOrSpoofingForwardedFor() throws Exception {
        for (int i = 0; i < 3; i++) {
            Browser browser = loggedIn(account());
            for (int j = 0; j < 2; j++) { assertThat(browser.change("Wrong123!", NEW).statusCode()).isEqualTo(400); }
        }
        Browser browser = loggedIn(account());
        var response = browser.client.send(HttpRequest.newBuilder(uri("/api/v1/users/me/password"))
                .header("Content-Type", "application/json").header("X-CSRF-TOKEN", browser.token)
                .header("X-Forwarded-For", "192.0.2.123")
                .PUT(HttpRequest.BodyPublishers.ofString(body(OLD, NEW))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(429);
    }

    @Test
    void openApiIncludesWriteOnlyPasswordsAndSessionRequirement() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/v3/api-docs")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var document = json.readTree(response.body());
        var endpoint = document.path("paths").path("/api/v1/users/me/password").path("put");
        assertThat(endpoint.path("security").get(0).has("sessionCookie")).isTrue();
        var properties = document.path("components").path("schemas").path("ChangePasswordRequest").path("properties");
        for (String field : List.of("oldPassword", "newPassword")) {
            assertThat(properties.path(field).path("writeOnly").asBoolean()).isTrue();
            assertThat(properties.path(field).path("maxLength").asInt()).isEqualTo(128);
        }
    }

    private String account() {
        String email = Long.toUnsignedString(ThreadLocalRandom.current().nextLong()) + "@bjtu.edu.cn";
        emails.add(email);
        jdbc.update("insert into foodtime.users(id,email,password_hash,display_name,email_verified_at) values (?, ?, ?, '干饭人123456', statement_timestamp())",
                UUID.randomUUID(), email, passwords.encode(OLD));
        return email;
    }
    private Browser loggedIn(String email) throws Exception {
        Browser browser = new Browser();
        browser.csrf();
        assertThat(browser.login(email, OLD).statusCode()).isEqualTo(200);
        browser.csrf();
        return browser;
    }
    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    private String body(String oldPassword, String newPassword) {
        return json.writeValueAsString(Map.of("oldPassword", oldPassword, "newPassword", newPassword));
    }
    private class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
        String token;
        void csrf() throws Exception {
            var response = get("/api/v1/auth/csrf");
            assertThat(response.statusCode()).isEqualTo(200);
            token = json.readTree(response.body()).path("data").path("token").asString();
        }
        HttpResponse<String> get(String path) throws Exception { return send("GET", path, null, false); }
        HttpResponse<String> login(String email, String password) throws Exception {
            return send("POST", "/api/v1/auth/login", json.writeValueAsString(Map.of("account", email, "password", password)), true);
        }
        HttpResponse<String> change(String oldPassword, String newPassword) throws Exception {
            return send("PUT", "/api/v1/users/me/password", body(oldPassword, newPassword), true);
        }
        HttpResponse<String> send(String method, String path, String payload, boolean csrf) throws Exception {
            var request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15));
            if (csrf) { request.header("X-CSRF-TOKEN", token); }
            if (payload != null) { request.header("Content-Type", "application/json"); }
            request.method(method, payload == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
        String cookie() {
            return SecurityConfig.SESSION_COOKIE + "=" + cookies.getCookieStore().getCookies().stream()
                    .filter(cookie -> cookie.getName().equals(SecurityConfig.SESSION_COOKIE)).findFirst().orElseThrow().getValue();
        }
    }
}
