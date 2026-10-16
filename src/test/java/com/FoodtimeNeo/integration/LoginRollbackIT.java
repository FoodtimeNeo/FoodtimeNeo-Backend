package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rating-summary.refresh-enabled=false")
class LoginRollbackIT {
    private static final String PREFIX = "foodtime:integration:login-rollback:" + UUID.randomUUID();
    private static final String PASSWORD = "Foodtime2026!";
    @Value("${local.server.port}") private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;
    @Autowired private PasswordEncoder passwords;
    @MockitoBean private SessionAuthenticationService authentication;
    private final JsonMapper json = JsonMapper.builder().build();
    private final UUID userId = UUID.randomUUID();

    @DynamicPropertySource
    static void isolatedNamespaces(DynamicPropertyRegistry properties) {
        properties.add("spring.session.redis.namespace", () -> PREFIX + ":sessions");
        properties.add("app.auth.redis-namespace", () -> PREFIX + ":limits");
    }

    @BeforeEach
    void failWhileCreatingTheLoginSession() {
        doCallRealMethod().when(authentication).clear(any());
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(1);
            request.getSession(true).setAttribute("partial-login", true);
            throw new RedisConnectionFailureException("simulated session write failure");
        }).when(authentication).establish(any(), any(), any());
    }

    @AfterEach
    void cleanOnlyThisTestData() {
        jdbc.update("delete from foodtime.users where id = ?", userId);
        Set<String> keys = redis.keys(PREFIX + ":*");
        if (keys != null && !keys.isEmpty()) { redis.delete(keys); }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedSessionCreationDoesNotCountAsASuccessfulLogin(boolean previouslyLoggedIn) throws Exception {
        String email = Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits()) + "@bjtu.edu.cn";
        Timestamp previousLogin = previouslyLoggedIn ? Timestamp.from(Instant.parse("2000-01-01T00:00:00Z")) : null;
        jdbc.update("""
                insert into foodtime.users (id, email, password_hash, display_name, last_login_at)
                values (?, ?, ?, 'test user', ?)
                """, userId, email, passwords.encode(PASSWORD), previousLogin);
        var browser = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        String base = "http://127.0.0.1:" + port + "/api/v1/auth";
        var csrf = browser.send(HttpRequest.newBuilder(URI.create(base + "/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String token = json.readTree(csrf.body()).path("data").path("token").asString();
        var response = browser.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                        .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(
                                Map.of("account", email, "password", PASSWORD)))).build(),
                HttpResponse.BodyHandlers.ofString());

        // Compare database state first: a failed login must preserve NULL or the previous success time.
        assertThat(jdbc.queryForObject("select last_login_at from foodtime.users where id = ?",
                Timestamp.class, userId)).isEqualTo(previousLogin);
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("AUTH_UNAVAILABLE");
        assertThat(browser.send(HttpRequest.newBuilder(URI.create(base + "/me")).GET().build(),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }
}
