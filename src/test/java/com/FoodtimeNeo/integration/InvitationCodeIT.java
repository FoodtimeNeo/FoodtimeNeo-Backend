package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.invitation.secret=0123456789abcdef0123456789abcdef0123456789abcdef"})
class InvitationCodeIT extends IntegrationApiSupport {
    private static final CapturingSmtpServer SMTP = new CapturingSmtpServer();
    private static final String EMAIL_PREFIX = "foodtime:integration:invitation-email:" + UUID.randomUUID();

    @DynamicPropertySource
    static void emailConfiguration(DynamicPropertyRegistry properties) {
        properties.add("app.email-verification.enabled", () -> true);
        properties.add("app.email-verification.from", () -> "sender@example.test");
        properties.add("app.email-verification.secret", () -> "test-only-secret-at-least-32-characters");
        properties.add("app.email-verification.redis-namespace", () -> EMAIL_PREFIX);
        properties.add("spring.mail.host", () -> "127.0.0.1");
        properties.add("spring.mail.port", SMTP::port);
        properties.add("spring.mail.properties[mail.smtp.localhost]", () -> "localhost");
        properties.add("spring.mail.properties[mail.from]", () -> "sender@example.test");
        properties.add("spring.mail.properties[mail.smtp.auth]", () -> false);
        properties.add("spring.mail.properties[mail.smtp.starttls.enable]", () -> false);
        properties.add("spring.mail.properties[mail.smtp.starttls.required]", () -> false);
        properties.add("spring.mail.properties[mail.smtp.ssl.enable]", () -> false);
    }

    @AfterAll
    static void closeSmtp() throws Exception { SMTP.close(); }

    private String emailCode(Browser browser, String email) throws Exception {
        assertThat(browser.call("POST", "/api/v1/auth/register/email-code", Map.of("email", email)).statusCode()).isEqualTo(202);
        return SMTP.code(email);
    }

    private record Created(UUID id, String code) { }

    @Test
    void generateAndListRequireSuperadmin() throws Exception {
        var user = login(owner);
        assertThat(user.call("POST", "/api/v1/admin/invitation-codes", Map.of("label", "x")).statusCode()).isEqualTo(403);
        var admin = login(account("admin"));
        assertThat(admin.call("POST", "/api/v1/admin/invitation-codes", Map.of("label", "x")).statusCode()).isEqualTo(403);
        var superadmin = login(account("superadmin"));
        var response = superadmin.call("POST", "/api/v1/admin/invitation-codes",
                Map.of("label", "test", "maxUses", 2));
        assertThat(response.statusCode()).isEqualTo(201);
        String code = body(response).path("data").path("code").asString();
        assertThat(code).matches("[A-HJ-NP-Z2-9]{12}");
        UUID id = UUID.fromString(body(response).path("data").path("id").asString());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.invitation_codes WHERE id = ? AND code_hash <> ?",
                Long.class, id, code)).isEqualTo(1L);
        var listed = body(superadmin.call("GET", "/api/v1/admin/invitation-codes", null)).path("data").path("items");
        var item = findById(listed, id);
        assertThat(item.path("maxUses").asInt()).isEqualTo(2);
        assertThat(item.path("usedCount").asInt()).isEqualTo(0);
        assertThat(item.path("code").isMissingNode()).isTrue();
        assertThat(superadmin.call("PUT", "/api/v1/admin/invitation-codes/" + id,
                Map.of("status", "disabled")).statusCode()).isEqualTo(200);
        assertThat(findById(body(superadmin.call("GET", "/api/v1/admin/invitation-codes?status=disabled", null))
                .path("data").path("items"), id).isMissingNode()).isFalse();
        assertThat(superadmin.call("GET", "/api/v1/admin/invitation-codes?status=nope", null).statusCode()).isEqualTo(400);
    }

    private static tools.jackson.databind.JsonNode findById(tools.jackson.databind.JsonNode items, UUID id) {
        for (var item : items) {
            if (item.path("id").asString().equals(id.toString())) return item;
        }
        return tools.jackson.databind.node.MissingNode.getInstance();
    }

    @Test
    void registerWithLowercaseCodeRecordsAttribution() throws Exception {
        var superadmin = login(account("superadmin"));
        Created created = create(superadmin, Map.of("maxUses", 2));
        Browser anonymous = new Browser(); anonymous.csrf();
        String email = freshEmail(0);
        var response = anonymous.call("POST", "/api/v1/auth/register",
                Map.of("email", email, "password", PASSWORD, "verificationCode", emailCode(anonymous, email),
                        "invitationCode", created.code().toLowerCase(java.util.Locale.ROOT)));
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT invited_by_code_id FROM foodtime.users WHERE lower(btrim(email)) = ?",
                UUID.class, email)).isEqualTo(created.id());
        assertThat(jdbc.queryForObject("SELECT used_count FROM foodtime.invitation_codes WHERE id = ?",
                Integer.class, created.id())).isEqualTo(1);
    }

    @Test
    void registerWithoutCodeIsUnaffected() throws Exception {
        Browser anonymous = new Browser(); anonymous.csrf();
        String email = freshEmail(0);
        var response = anonymous.call("POST", "/api/v1/auth/register",
                Map.of("email", email, "password", PASSWORD, "verificationCode", emailCode(anonymous, email)));
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT invited_by_code_id FROM foodtime.users WHERE lower(btrim(email)) = ?",
                UUID.class, email)).isNull();
    }

    @Test
    void malformedCodeIsRejectedAtTheProtocol() throws Exception {
        Browser anonymous = new Browser(); anonymous.csrf();
        var response = anonymous.call("POST", "/api/v1/auth/register",
                Map.of("email", freshEmail(0), "password", PASSWORD, "verificationCode", "000000",
                        "invitationCode", "SHORT"));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("invitationCode");
        assertThat(response.body()).doesNotContain("INVITATION_CODE_INVALID");
    }

    @Test
    void unknownExpiredDisabledOrExhaustedCodesAreRejected() throws Exception {
        var superadmin = login(account("superadmin"));
        Browser anonymous = new Browser(); anonymous.csrf();
        String unknownEmail = freshEmail(0);
        var unknown = anonymous.call("POST", "/api/v1/auth/register",
                Map.of("email", unknownEmail, "password", PASSWORD, "verificationCode", emailCode(anonymous, unknownEmail),
                        "invitationCode", "AAAAAAAAAAAA"));
        assertThat(unknown.statusCode()).isEqualTo(400);
        assertThat(unknown.body()).contains("INVITATION_CODE_INVALID");

        Created limited = create(superadmin, Map.of("maxUses", 1));
        assertThat(registerWith(anonymous, limited.code(), 1).statusCode()).isEqualTo(201);
        var exhausted = registerWith(anonymous, limited.code(), 2);
        assertThat(exhausted.statusCode()).isEqualTo(400);
        assertThat(exhausted.body()).contains("INVITATION_CODE_INVALID");

        Created expirable = create(superadmin, Map.of("maxUses", 5));
        jdbc.update("UPDATE foodtime.invitation_codes SET expires_at = now() - interval '1 second' WHERE id = ?", expirable.id());
        assertThat(registerWith(anonymous, expirable.code(), 3).statusCode()).isEqualTo(400);

        Created stoppable = create(superadmin, Map.of("maxUses", 5));
        assertThat(superadmin.call("PUT", "/api/v1/admin/invitation-codes/" + stoppable.id(),
                Map.of("status", "disabled")).statusCode()).isEqualTo(200);
        assertThat(registerWith(anonymous, stoppable.code(), 4).statusCode()).isEqualTo(400);
        assertThat(superadmin.call("PUT", "/api/v1/admin/invitation-codes/" + UUID.randomUUID(),
                Map.of("status", "disabled")).statusCode()).isEqualTo(404);
    }

    private Created create(Browser superadmin, Map<String, Object> body) throws Exception {
        var response = superadmin.call("POST", "/api/v1/admin/invitation-codes", body);
        assertThat(response.statusCode()).isEqualTo(201);
        return new Created(UUID.fromString(body(response).path("data").path("id").asString()),
                body(response).path("data").path("code").asString());
    }

    private java.net.http.HttpResponse<String> registerWith(Browser anonymous, String code, int salt) throws Exception {
        String email = freshEmail(salt);
        return anonymous.call("POST", "/api/v1/auth/register",
                Map.of("email", email, "password", PASSWORD, "verificationCode", emailCode(anonymous, email),
                        "invitationCode", code));
    }

    private static String freshEmail(int salt) {
        return salt + Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits()) + "@bjtu.edu.cn";
    }
}
