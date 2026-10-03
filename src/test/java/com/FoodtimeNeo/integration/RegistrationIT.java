package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.FoodtimeNeo.auth.verification.EmailVerificationStore;
import com.FoodtimeNeo.auth.verification.EmailVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rating-summary.refresh-enabled=false", "app.email-verification.send-ip-limit=6"})
class RegistrationIT {
    private static final CapturingSmtpServer SMTP = new CapturingSmtpServer();
    private static final String PREFIX = "foodtime:integration:email:" + UUID.randomUUID();

    @DynamicPropertySource
    static void mailConfiguration(DynamicPropertyRegistry properties) {
        properties.add("app.email-verification.enabled", () -> true);
        properties.add("app.email-verification.from", () -> "sender@example.test");
        properties.add("app.email-verification.secret", () -> "test-only-secret-at-least-32-characters");
        properties.add("app.email-verification.redis-namespace", () -> PREFIX);
        properties.add("spring.session.redis.namespace", () -> PREFIX + ":sessions");
        properties.add("app.auth.redis-namespace", () -> PREFIX + ":login");
        properties.add("spring.mail.host", () -> "127.0.0.1");
        properties.add("spring.mail.port", SMTP::port);
        properties.add("spring.mail.properties[mail.smtp.auth]", () -> false);
        properties.add("spring.mail.properties[mail.smtp.starttls.enable]", () -> false);
        properties.add("spring.mail.properties[mail.smtp.starttls.required]", () -> false);
        properties.add("spring.mail.properties[mail.smtp.ssl.enable]", () -> false);
    }

    @AfterAll
    static void closeSmtp() throws Exception { SMTP.close(); }
    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwords;
    @Autowired private StringRedisTemplate redis;
    @Autowired private EmailVerificationStore store;
    @Autowired private EmailVerificationService verification;
    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final List<String> testEmails = new ArrayList<>();
    private final Map<String, String> codes = new HashMap<>();

    @AfterEach
    void removeOnlyAccountsCreatedByThisTest() {
        for (String email : testEmails) {
            jdbc.update("delete from foodtime.users where email = ?", email);
        }
        Set<String> keys = redis.keys(PREFIX + ":*");
        if (keys != null && !keys.isEmpty()) { redis.delete(keys); }
        SMTP.clear();
    }

    @Test
    void registrationPersistsSaltedHashAndDiscardsClientControlledPrivileges() throws Exception {
        String email = newEmail();
        String password = "aB12!" + "x".repeat(80) + "~";
        String payload = json.writeValueAsString(Map.of("email", " " + email.toUpperCase() + " ", "password", password,
                "role", "superadmin", "displayName", "attacker chosen", "status", "disabled", "verificationCode", sendCode(email)));
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
        assertThat(user.get("email_verified_at")).isNotNull();
        assertThat(redis.hasKey(store.key("code", email))).isFalse();
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
        assertThat(document.path("paths").has("/api/v1/auth/register/email-code")).isTrue();
        assertThat(document.path("components").path("schemas").path("RegisterRequest")
                .path("properties").path("verificationCode").path("writeOnly").asBoolean()).isTrue();
    }

    @Test
    void sendCodeUsesConfiguredSenderStoresOnlyProofAndExpiresAfter15Minutes() throws Exception {
        String email = newEmail();
        var response = send(email);
        assertThat(response.statusCode()).isEqualTo(202);
        String code = SMTP.code(email);
        assertThat(code).matches("[0-9]{6}");
        assertThat(SMTP.message(email).getFrom()[0].toString()).isEqualTo("sender@example.test");
        assertThat(json.readTree(response.body()).path("data").has("verificationCode")).isFalse();
        assertThat(redis.getExpire(store.key("code", email))).isBetween(895L, 900L);
        Object proof = redis.opsForHash().get(store.key("code", email), "proof");
        assertThat(proof.toString()).hasSize(64).isNotEqualTo(code);
        assertThat(store.key("code", email)).doesNotContain(email);
        assertThat(jdbc.queryForObject("select count(*) from foodtime.users where email = ?", Integer.class, email)).isZero();
    }

    @Test
    void sendingRequiresCsrfAndRepeatedSendsReturnRetryAfter() throws Exception {
        String email = newEmail();
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/auth/register/email-code"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("email", email)))).build();
        assertThat(http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
        assertThat(SMTP.message(email)).isNull();
        assertThat(send(email).statusCode()).isEqualTo(202);
        var repeated = send(email);
        assertThat(repeated.statusCode()).isEqualTo(429);
        assertThat(json.readTree(repeated.body()).path("code").asString()).isEqualTo("EMAIL_SEND_RATE_LIMITED");
        assertThat(Long.parseLong(repeated.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L, 60L);
    }

    @Test
    void smtpFailureDoesNotPublishCodeAndReleasesCooldownForRetry() throws Exception {
        String email = newEmail();
        SMTP.rejectNext.set(true);
        var failed = send(email);
        assertThat(failed.statusCode()).isEqualTo(503);
        assertThat(json.readTree(failed.body()).path("code").asString()).isEqualTo("EMAIL_VERIFICATION_UNAVAILABLE");
        assertThat(redis.hasKey(store.key("code", email))).isFalse();
        assertThat(redis.hasKey(store.key("cooldown", email))).isFalse();
        assertThat(send(email).statusCode()).isEqualTo(202);
    }

    @Test
    void wrongCodeForAnotherEmailAndExpiredCodesNeverCreateAccounts() throws Exception {
        String owner = newEmail();
        String code = sendCode(owner);
        String other = newEmail();
        assertCodeInvalid(registerWithCode(other, code));
        redis.delete(store.key("code", owner));
        assertCodeInvalid(registerWithCode(owner, code));
        assertThat(jdbc.queryForObject("select count(*) from foodtime.users where email in (?, ?)", Integer.class, owner, other)).isZero();
    }

    @Test
    void fiveWrongAttemptsInvalidateCodeWithoutExtendingTtl() throws Exception {
        String email = newEmail();
        String code = sendCode(email);
        String wrong = code.equals("000000") ? "000001" : "000000";
        long initial = redis.getExpire(store.key("code", email));
        for (int attempt = 0; attempt < 5; attempt++) {
            assertCodeInvalid(registerWithCode(email, wrong));
            if (attempt < 4) { assertThat(redis.getExpire(store.key("code", email))).isLessThanOrEqualTo(initial); }
        }
        assertThat(redis.hasKey(store.key("code", email))).isFalse();
        assertCodeInvalid(registerWithCode(email, code));
        assertThat(jdbc.queryForObject("select count(*) from foodtime.users where email = ?", Integer.class, email)).isZero();
    }

    @Test
    void resendingReplacesOldProofAndFailedResendsKeepPreviousProof() throws Exception {
        String email = newEmail();
        String original = sendCode(email);
        Object oldProof = redis.opsForHash().get(store.key("code", email), "proof");
        redis.delete(store.key("cooldown", email));
        SMTP.rejectNext.set(true);
        assertThat(send(email).statusCode()).isEqualTo(503);
        assertThat(redis.opsForHash().get(store.key("code", email), "proof")).isEqualTo(oldProof);
        assertThat(send(email).statusCode()).isEqualTo(202);
        String replacement = SMTP.code(email);
        if (!replacement.equals(original)) { assertCodeInvalid(registerWithCode(email, original)); }
        assertThat(registerWithCode(email, replacement).statusCode()).isEqualTo(201);
    }

    @Test
    void releasedClaimCanRetryAndStaleFinalizationCannotRemoveNewerCode() throws Exception {
        String email = newEmail();
        String code = sendCode(email);
        var claim = verification.claim(email, code);
        assertThat(registerWithCode(email, code).statusCode()).isEqualTo(409);
        verification.finish(claim, false);
        var second = verification.claim(email, code);
        verification.finish(claim, true);
        assertThat(redis.hasKey(store.key("code", email))).isTrue();
        verification.finish(second, false);
        assertThat(registerWithCode(email, code).statusCode()).isEqualTo(201);
    }

    @Test
    void simultaneousSendRequestsAcceptExactlyOneMessage() throws Exception {
        String email = newEmail();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return send(email).statusCode(); });
            var second = executor.submit(() -> { start.await(); return send(email).statusCode(); });
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(202, 429);
        }
        assertThat(SMTP.code(email)).matches("[0-9]{6}");
    }

    @Test
    void mailboxAndIpSendQuotasCannotBeBypassedByClearingCooldownOrChangingEmail() throws Exception {
        String email = newEmail();
        for (int i = 0; i < 5; i++) {
            assertThat(send(email).statusCode()).isEqualTo(202);
            redis.delete(store.key("cooldown", email));
        }
        var limited = send(email);
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(Long.parseLong(limited.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L, 900L);
        assertThat(send(newEmail()).statusCode()).isEqualTo(202); // sixth accepted send from the IP
        String seventh = newEmail();
        assertThat(send(seventh).statusCode()).isEqualTo(429);
        assertThat(SMTP.message(seventh)).isNull();
    }

    @Test
    void verifiedRegistrationCanLoginRestoreIdentityAndLogoutThroughHttp() throws Exception {
        String email = newEmail();
        String password = "Foodtime2026!";
        HttpResponse<String> registered = register(body(email, password));
        assertThat(registered.statusCode()).isEqualTo(201);
        String userId = json.readTree(registered.body()).path("data").path("id").asString();

        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var browser = HttpClient.newBuilder().cookieHandler(cookies).build();
        String base = "http://127.0.0.1:" + port + "/api/v1/auth";
        var csrfRequest = HttpRequest.newBuilder(URI.create(base + "/csrf")).GET().build();
        var csrf = browser.send(csrfRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String token = json.readTree(csrf.body()).path("data").path("token").asString();
        var loginRequest = HttpRequest.newBuilder(URI.create(base + "/login"))
                .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(
                        Map.of("account", email, "password", password)), StandardCharsets.UTF_8)).build();
        var login = browser.send(loginRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(json.readTree(login.body()).path("data").path("user").path("id").asString()).isEqualTo(userId);

        var meRequest = HttpRequest.newBuilder(URI.create(base + "/me")).GET().build();
        var me = browser.send(meRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(json.readTree(me.body()).path("data").path("email").asString()).isEqualTo(email);
        assertThat(json.readTree(me.body()).path("data").path("role").asString()).isEqualTo("user");

        var loginCsrf = browser.send(csrfRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String loginToken = json.readTree(loginCsrf.body()).path("data").path("token").asString();
        assertThat(loginToken).isNotEqualTo(token);
        var logoutRequest = HttpRequest.newBuilder(URI.create(base + "/logout"))
                .header("X-CSRF-TOKEN", loginToken).POST(HttpRequest.BodyPublishers.noBody()).build();
        assertThat(browser.send(logoutRequest, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        assertThat(browser.send(meRequest, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }

    private void assertCodeInvalid(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("EMAIL_CODE_INVALID");
    }

    private HttpResponse<String> registerWithCode(String email, String code) throws Exception {
        return register(json.writeValueAsString(Map.of("email", email, "password", "Abc12345", "verificationCode", code)));
    }

    private String sendCode(String email) throws Exception {
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (!codes.containsKey(normalized)) {
            assertThat(send(normalized).statusCode()).isEqualTo(202);
            codes.put(normalized, SMTP.code(normalized));
        }
        return codes.get(normalized);
    }

    private HttpResponse<String> send(String email) throws Exception {
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var browser = HttpClient.newBuilder().cookieHandler(cookies).build();
        String base = "http://127.0.0.1:" + port + "/api/v1/auth";
        var csrf = browser.send(HttpRequest.newBuilder(URI.create(base + "/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        String token = json.readTree(csrf.body()).path("data").path("token").asString();
        return browser.send(HttpRequest.newBuilder(URI.create(base + "/register/email-code"))
                .header("Content-Type", "application/json").header("X-CSRF-TOKEN", token)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("email", email)), StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String newEmail() {
        String email = Long.toUnsignedString(ThreadLocalRandom.current().nextLong()) + "@bjtu.edu.cn";
        testEmails.add(email);
        return email;
    }

    private String body(String email, String password) throws Exception {
        return json.writeValueAsString(Map.of("email", email, "password", password, "verificationCode", sendCode(email)));
    }

    private HttpResponse<String> register(String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/auth/register"))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
