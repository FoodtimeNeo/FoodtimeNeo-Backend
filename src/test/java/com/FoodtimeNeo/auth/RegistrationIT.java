package com.FoodtimeNeo.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rating-summary.refresh-enabled=false")
class RegistrationIT {
    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwords;
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final List<String> testEmails = new ArrayList<>();

    @AfterEach
    void removeOnlyAccountsCreatedByThisTest() {
        for (String email : testEmails) {
            jdbc.update("delete from foodtime.users where email = ?", email);
        }
    }

    @Test
    void registrationPersistsSaltedHashAndDiscardsClientControlledPrivileges() throws Exception {
        String email = newEmail();
        String password = "aB12!" + "x".repeat(80) + "~";
        String payload = json.writeValueAsString(Map.of("email", " " + email.toUpperCase() + " ", "password", password,
                "role", "superadmin", "displayName", "attacker chosen", "status", "disabled"));
        HttpResponse<String> response = register(payload);
        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode body = json.readTree(response.body());
        assertThat(body.path("data").path("email").asString()).isEqualTo(email);
        assertThat(body.path("data").path("displayName").asString()).matches("干饭人[0-9]{6}");
        assertThat(body.path("data").path("role").asString()).isEqualTo("user");
        assertThat(body.path("data").has("password")).isFalse();
        assertThat(body.path("data").has("passwordHash")).isFalse();
        assertThat(response.body()).doesNotContain("argon2", password, "attacker chosen");
        Map<String, Object> user = jdbc.queryForMap("select * from foodtime.users where email = ?", email);
        String hash = (String) user.get("password_hash");
        assertThat(hash).startsWith("{argon2id}$argon2id$").isNotEqualTo(password);
        assertThat(passwords.matches(password, hash)).isTrue();
        assertThat(passwords.matches(password.substring(0, password.length() - 1) + "!", hash)).isFalse();
        assertThat(user.get("display_name")).isEqualTo(body.path("data").path("displayName").asString());
        assertThat(user.get("role")).isEqualTo("user");
        assertThat(user.get("status")).isEqualTo("active");
        assertThat(user.get("email_verified_at")).isNull();
        assertThat(user.get("created_at")).isNotNull();
        assertThat(user.get("id").toString()).isEqualTo(body.path("data").path("id").asString());
    }

    @Test
    void aDuplicateRegistrationDoesNotOverwriteTheExistingAccount() throws Exception {
        String email = newEmail();
        assertThat(register(body(email, "Abc12345")).statusCode()).isEqualTo(201);
        String originalHash = jdbc.queryForObject("select password_hash from foodtime.users where email = ?", String.class, email);
        HttpResponse<String> duplicate = register(body(email.toUpperCase(), "Different123"));
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(json.readTree(duplicate.body()).path("code").asString()).isEqualTo("EMAIL_ALREADY_REGISTERED");
        assertThat(jdbc.queryForObject("select password_hash from foodtime.users where email = ?", String.class, email))
                .isEqualTo(originalHash);
        assertThat(jdbc.queryForObject("select count(*) from foodtime.users where email = ?", Integer.class, email)).isEqualTo(1);
    }

    @Test
    void simultaneousRequestsCreateExactlyOneAccount() throws Exception {
        String email = newEmail();
        String payload = body(email, "Abc12345");
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return register(payload).statusCode(); });
            var second = executor.submit(() -> { start.await(); return register(payload).statusCode(); });
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("select count(*) from foodtime.users where email = ?", Integer.class, email)).isEqualTo(1);
    }

    @Test
    void invalidPasswordsReturnFieldErrorsAndLeaveTheDatabaseUntouched() throws Exception {
        String email = newEmail();
        HttpResponse<String> response = register(body(email, "abcdefgh"));
        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode body = json.readTree(response.body());
        assertThat(body.path("code").asString()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.path("data").get(0).path("field").asString()).isEqualTo("password");
        assertThat(response.body()).doesNotContain("abcdefgh");
        assertThat(jdbc.queryForObject("select count(*) from foodtime.users where email = ?", Integer.class, email)).isZero();
    }

    @Test
    void disallowedCharactersAndMalformedUnicodeNeverCreateAccounts() throws Exception {
        String email = newEmail();
        for (String password : List.of("A1😀😀😀", "Abc12345中文", "Abc12345 ")) {
            HttpResponse<String> response = register(body(email, password));
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("VALIDATION_ERROR");
        }
        HttpResponse<String> malformed = register("{\"email\":\"" + email + "\",\"password\":\"Abc12345\\ud800\"}");
        assertThat(malformed.statusCode()).isEqualTo(400);
        assertThat(json.readTree(malformed.body()).path("code").asString()).isEqualTo("VALIDATION_ERROR");
        assertThat(jdbc.queryForObject("select count(*) from foodtime.users where email = ?", Integer.class, email)).isZero();
    }

    @Test
    void openApiIncludesRegistrationAndTheWriteOnlyPasswordContract() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/v3/api-docs")).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode document = json.readTree(response.body());
        assertThat(document.path("paths").has("/api/v1/auth/register")).isTrue();
        JsonNode password = document.path("components").path("schemas").path("RegisterRequest").path("properties").path("password");
        assertThat(password.path("writeOnly").asBoolean()).isTrue();
        assertThat(password.path("minLength").asInt()).isEqualTo(8);
        assertThat(password.path("description").asString()).contains("ASCII半角符号", "不允许空白");
    }

    private String newEmail() {
        String email = Long.toUnsignedString(ThreadLocalRandom.current().nextLong()) + "@bjtu.edu.cn";
        testEmails.add(email);
        return email;
    }

    private String body(String email, String password) {
        return json.writeValueAsString(Map.of("email", email, "password", password));
    }

    private HttpResponse<String> register(String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/auth/register"))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
