package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.auth.security.SessionPrincipal;
import com.FoodtimeNeo.auth.service.LoginRateLimiter;
import com.FoodtimeNeo.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rating-summary.refresh-enabled=false", "app.auth.login-account-limit=3",
                "app.auth.login-ip-limit=6"})
class LoginIT {
    private static final String PREFIX = "foodtime:integration:login:" + UUID.randomUUID();
    private static final String PASSWORD = "Foodtime2026!";
    @Value("${local.server.port}") private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private LoginRateLimiter limiter;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwords;
    @Autowired private SessionRepository<? extends Session> sessions;
    private final JsonMapper json = JsonMapper.builder().build();
    private final List<String> emails = new ArrayList<>();

    @DynamicPropertySource
    static void isolatedRedisNamespaces(DynamicPropertyRegistry properties) {
        properties.add("spring.session.redis.namespace", () -> PREFIX + ":sessions");
        properties.add("app.auth.redis-namespace", () -> PREFIX + ":limits");
    }

    @AfterEach
    void cleanOnlyThisRunData() {
        for (String email : emails) { jdbc.update("delete from foodtime.users where email = ?", email); }
        Set<String> keys = redis.keys(PREFIX + ":*");
        if (keys != null && !keys.isEmpty()) { redis.delete(keys); }
    }

    @Test
    void loginPersistsAcrossRequestsAndCookieContainsOnlyAnOpaqueSessionId() throws Exception {
        String email = registeredEmail();
        Browser browser = new Browser();
        browser.csrf();
        String anonymousId = browser.sessionId();
        Instant before = Instant.now();
        HttpResponse<String> response = browser.login(" " + email.toUpperCase(Locale.ROOT) + " ", PASSWORD);
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode data = json.readTree(response.body()).path("data");
        assertThat(data.path("user").path("email").asString()).isEqualTo(email);
        assertThat(data.path("user").path("role").asString()).isEqualTo("user");
        assertThat(response.body()).doesNotContain(PASSWORD, "passwordHash", "argon2");
        Instant expiry = Instant.parse(data.path("expiresAt").asString());
        assertThat(expiry).isBetween(before.plus(Duration.ofDays(7)), Instant.now().plus(Duration.ofDays(7)));
        assertThat(response.headers().firstValue("Set-Cookie").orElseThrow())
                .contains(SecurityConfig.SESSION_COOKIE + "=", "HttpOnly", "SameSite=Lax", "Path=/", "Max-Age=604800")
                .doesNotContain("Domain=");
        assertThat(browser.sessionId()).isNotEqualTo(anonymousId);
        assertThat(sessions.findById(anonymousId)).isNull();
        Session saved = sessions.findById(browser.sessionId());
        assertThat(saved).isNotNull();
        SecurityContext context = saved.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().getCredentials()).isNull();
        assertThat(context.getAuthentication().getPrincipal()).isInstanceOf(SessionPrincipal.class);
        HttpResponse<String> me = browser.send("GET", "/me", null, false);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(json.readTree(me.body()).path("data").path("email").asString()).isEqualTo(email);
        assertThat(me.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(jdbc.queryForObject("select last_login_at from foodtime.users where email = ?", Object.class, email)).isNotNull();
        // A fresh HTTP client can restore a session using only the saved browser cookie.
        String cookie = browser.cookieHeader();
        assertThat(HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/me"))
                .header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
    }

    @Test
    void missingOrWrongCsrfIsRejectedAndSuccessfulLoginRotatesTheToken() throws Exception {
        String email = registeredEmail();
        Browser browser = new Browser();
        assertThat(browser.send("POST", "/login", loginBody(email, PASSWORD), false).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("select last_login_at from foodtime.users where email = ?", Object.class, email)).isNull();
        browser.csrf();
        String oldToken = browser.token;
        browser.token = "invalid-token";
        assertThat(browser.login(email, PASSWORD).statusCode()).isEqualTo(403);
        browser.token = oldToken;
        assertThat(browser.login(email, PASSWORD).statusCode()).isEqualTo(200);
        assertThat(browser.send("POST", "/logout", null, true).statusCode()).isEqualTo(403);
        assertThat(browser.send("GET", "/me", null, false).statusCode()).isEqualTo(200);
        browser.csrf();
        assertThat(browser.token).isNotEqualTo(oldToken);
        assertThat(browser.send("POST", "/logout", null, true).statusCode()).isEqualTo(200);
    }

    @Test
    void wrongUnknownAndDisabledAccountsHaveTheSame401AndNeverReceiveAuthSessions() throws Exception {
        String email = registeredEmail();
        String unknown = newEmail();
        Browser browser = new Browser();
        browser.csrf();
        String anonymousId = browser.sessionId();
        for (String[] attempt : List.of(new String[]{email, "Wrong123!"}, new String[]{unknown, PASSWORD})) {
            assertInvalidCredentials(browser.login(attempt[0], attempt[1]));
        }
        jdbc.update("update foodtime.users set status = 'disabled' where email = ?", email);
        assertInvalidCredentials(browser.login(email, PASSWORD));
        assertThat(browser.sessionId()).isEqualTo(anonymousId);
        assertThat(browser.send("GET", "/me", null, false).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("select last_login_at from foodtime.users where email = ?", Object.class, email)).isNull();
    }

    @Test
    void logoutDeletesRedisSessionAndTheCookieCannotBeReplayed() throws Exception {
        Browser browser = loggedIn(registeredEmail());
        String id = browser.sessionId();
        String stolenCookie = browser.cookieHeader();
        browser.csrf();
        HttpResponse<String> logout = browser.send("POST", "/logout", null, true);
        assertThat(logout.statusCode()).isEqualTo(200);
        assertThat(logout.headers().firstValue("Set-Cookie").orElseThrow()).contains("Max-Age=0");
        assertThat(sessions.findById(id)).isNull();
        assertThat(browser.send("GET", "/me", null, false).statusCode()).isEqualTo(401);
        assertThat(HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/me"))
                .header("Cookie", stolenCookie).GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }

    @Test
    void subsequentLoginReplacesTheExistingSession() throws Exception {
        String email = registeredEmail();
        Browser browser = loggedIn(email);
        String oldId = browser.sessionId();
        browser.csrf();
        assertThat(browser.login(email, PASSWORD).statusCode()).isEqualTo(200);
        assertThat(browser.sessionId()).isNotEqualTo(oldId);
        assertThat(sessions.findById(oldId)).isNull();
    }

    @Test
    void passwordChangeInvalidatesTheExistingCookie() throws Exception {
        String email = registeredEmail();
        Browser browser = loggedIn(email);
        String id = browser.sessionId();
        jdbc.update("update foodtime.users set password_changed_at = password_changed_at + interval '1 second' where email = ?", email);
        assertThat(browser.send("GET", "/me", null, false).statusCode()).isEqualTo(401);
        assertThat(sessions.findById(id)).isNull();
    }

    @Test
    void disabledUsersLoseExistingAccessAndRoleChangesAreImmediatelyVisible() throws Exception {
        String email = registeredEmail();
        Browser browser = loggedIn(email);
        jdbc.update("update foodtime.users set role = 'admin' where email = ?", email);
        HttpResponse<String> me = browser.send("GET", "/me", null, false);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(json.readTree(me.body()).path("data").path("role").asString()).isEqualTo("admin");
        jdbc.update("update foodtime.users set status = 'disabled' where email = ?", email);
        assertThat(browser.send("GET", "/me", null, false).statusCode()).isEqualTo(401);
    }

    @Test
    void expiredAndForgedSessionIdsCannotAuthenticate() throws Exception {
        Browser browser = loggedIn(registeredEmail());
        String id = browser.sessionId();
        expirePrincipal(sessions, id);
        assertThat(browser.send("GET", "/me", null, false).statusCode()).isEqualTo(401);
        assertThat(sessions.findById(id)).isNull();
        assertThat(HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/me"))
                .header("Cookie", SecurityConfig.SESSION_COOKIE + "=forged-session").GET().build(),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }

    @Test
    void accountThrottlingIsSharedAcrossBrowsersAndKeepsAnExpiringCounter() throws Exception {
        String email = registeredEmail();
        for (int i = 0; i < 3; i++) {
            Browser browser = new Browser();
            browser.csrf();
            assertInvalidCredentials(browser.login(email, "Wrong123!"));
        }
        Browser browser = new Browser();
        browser.csrf();
        HttpResponse<String> limited = browser.login(email, PASSWORD);
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(json.readTree(limited.body()).path("code").asString()).isEqualTo("LOGIN_RATE_LIMITED");
        assertThat(Long.parseLong(limited.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L, 900L);
        assertThat(redis.getExpire(limiter.key("account", email))).isBetween(1L, 900L);
        assertThat(limiter.key("account", email)).doesNotContain(email);
    }

    @Test
    void ipThrottlingCannotBeBypassedBySpoofingForwardedFor() throws Exception {
        Browser browser = new Browser();
        browser.csrf();
        for (int i = 0; i < 6; i++) { assertInvalidCredentials(browser.login(newEmail(), PASSWORD)); }
        HttpResponse<String> limited = browser.client.send(HttpRequest.newBuilder(uri("/login"))
                .header("Content-Type", "application/json").header("X-CSRF-TOKEN", browser.token)
                .header("X-Forwarded-For", "192.0.2.123")
                .POST(HttpRequest.BodyPublishers.ofString(loginBody(newEmail(), PASSWORD))).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(limited.statusCode()).isEqualTo(429);
    }

    @Test
    void concurrentAttemptsCannotOvershootTheAtomicAccountLimit() throws Exception {
        String email = registeredEmail();
        List<Browser> browsers = new ArrayList<>();
        for (int i = 0; i < 5; i++) { Browser browser = new Browser(); browser.csrf(); browsers.add(browser); }
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(5)) {
            List<Future<Integer>> responses = new ArrayList<>();
            for (Browser browser : browsers) {
                responses.add(executor.submit(() -> { start.await(); return browser.login(email, "Wrong123!").statusCode(); }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> response : responses) { statuses.add(response.get(15, TimeUnit.SECONDS)); }
            assertThat(statuses).containsExactlyInAnyOrder(401, 401, 401, 429, 429);
        }
    }

    @Test
    void corsAllowsCredentialsOnlyForConfiguredOriginsAndApiDocsIncludeCookieScheme() throws Exception {
        var http = HttpClient.newHttpClient();
        HttpResponse<String> allowed = http.send(HttpRequest.newBuilder(uri("/login"))
                .header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,X-CSRF-TOKEN")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(allowed.statusCode()).isEqualTo(200);
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
        HttpResponse<String> denied = http.send(HttpRequest.newBuilder(uri("/csrf"))
                .header("Origin", "https://untrusted.example").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(denied.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        HttpResponse<String> docs = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/v3/api-docs"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        JsonNode document = json.readTree(docs.body());
        assertThat(document.path("paths").has("/api/v1/auth/login")).isTrue();
        assertThat(document.path("components").path("securitySchemes").path("sessionCookie").path("in").asString()).isEqualTo("cookie");
    }

    private <S extends Session> void expirePrincipal(SessionRepository<S> repository, String id) {
        S session = repository.findById(id);
        SecurityContext original = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        SessionPrincipal principal = (SessionPrincipal) original.getAuthentication().getPrincipal();
        SecurityContext expired = SecurityContextHolder.createEmptyContext();
        expired.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new SessionPrincipal(principal.userId(), principal.passwordChangedAt(), Instant.now().minusSeconds(1)),
                null, original.getAuthentication().getAuthorities()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, expired);
        repository.save(session);
    }

    private String registeredEmail() throws Exception {
        String email = newEmail();
        // Login tests use an already-verified account fixture; registration's SMTP flow is tested separately.
        jdbc.update("""
                insert into foodtime.users(id,email,password_hash,display_name,email_verified_at)
                values (?, ?, ?, '干饭人123456', statement_timestamp())
                """, UUID.randomUUID(), email, passwords.encode(PASSWORD));
        return email;
    }

    private String newEmail() {
        String email = Long.toUnsignedString(ThreadLocalRandom.current().nextLong()) + "@bjtu.edu.cn";
        emails.add(email);
        return email;
    }

    private Browser loggedIn(String email) throws Exception {
        Browser browser = new Browser();
        browser.csrf();
        assertThat(browser.login(email, PASSWORD).statusCode()).isEqualTo(200);
        return browser;
    }

    private void assertInvalidCredentials(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(401);
        JsonNode body = json.readTree(response.body());
        assertThat(body.path("code").asString()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(body.path("message").asString()).isEqualTo("账号或密码错误");
    }

    private URI uri(String path) { return URI.create("http://127.0.0.1:" + port + "/api/v1/auth" + path); }
    private String loginBody(String email, String password) { return json.writeValueAsString(Map.of("account", email, "password", password)); }

    private class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
        String token;

        void csrf() throws Exception {
            HttpResponse<String> response = send("GET", "/csrf", null, false);
            assertThat(response.statusCode()).isEqualTo(200);
            JsonNode data = json.readTree(response.body()).path("data");
            assertThat(data.path("headerName").asString()).isEqualTo("X-CSRF-TOKEN");
            token = data.path("token").asString();
        }

        HttpResponse<String> login(String email, String password) throws Exception {
            return send("POST", "/login", loginBody(email, password), true);
        }

        HttpResponse<String> send(String method, String path, String body, boolean csrf) throws Exception {
            var request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15));
            if (csrf) { request.header("X-CSRF-TOKEN", token); }
            if (body != null) { request.header("Content-Type", "application/json"); }
            request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        String cookieValue() {
            return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals(SecurityConfig.SESSION_COOKIE))
                    .findFirst().orElseThrow().getValue();
        }
        String sessionId() { return new String(Base64.getDecoder().decode(cookieValue()), StandardCharsets.UTF_8); }
        String cookieHeader() { return SecurityConfig.SESSION_COOKIE + "=" + cookieValue(); }
    }
}
