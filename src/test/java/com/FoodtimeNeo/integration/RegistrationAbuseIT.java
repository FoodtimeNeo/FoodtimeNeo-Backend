package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.auth.registration.account-limit=2", "app.auth.registration.ip-limit=3",
        "app.email-verification.enabled=true", "app.email-verification.from=sender@example.test",
        "app.email-verification.secret=test-only-secret-at-least-32-characters",
        "spring.mail.host=127.0.0.1", "spring.mail.port=1", "spring.mail.properties[mail.smtp.auth]=false"})
class RegistrationAbuseIT extends IntegrationApiSupport {
    private Map<String,String> request(String email) {
        return Map.of("email", email, "password", PASSWORD, "verificationCode", "000000");
    }
    @Test
    void wrongCodesDoNotDiscloseExistingEmailsAndAccountLimitsAlsoCoverSending() throws Exception {
        Browser browser=new Browser(); browser.csrf();
        var existing=browser.call("POST","/api/v1/auth/register",request(owner.email()));
        var unknown=browser.call("POST","/api/v1/auth/register",request("9"+Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits())+"@bjtu.edu.cn"));
        assertThat(existing.statusCode()).isEqualTo(400);
        assertThat(unknown.statusCode()).isEqualTo(400);
        assertThat(body(existing).path("code").asString()).isEqualTo("EMAIL_CODE_INVALID");
        assertThat(body(unknown).path("code").asString()).isEqualTo("EMAIL_CODE_INVALID");
        assertThat(browser.call("POST","/api/v1/auth/register",request(owner.email())).statusCode()).isEqualTo(400);
        var blocked=browser.call("POST","/api/v1/auth/register/email-code",Map.of("email",owner.email()));
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(body(blocked).path("code").asString()).isEqualTo("REGISTRATION_RATE_LIMITED");
        assertThat(Long.parseLong(blocked.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L,900L);
    }
    @Test
    void changingEmailsCannotBypassTheIpQuota() throws Exception {
        Browser browser=new Browser();
        for(int i=0;i<3;i++) assertThat(browser.call("POST","/api/v1/auth/register",request(i+Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits())+"@bjtu.edu.cn")).statusCode()).isEqualTo(400);
        assertThat(browser.call("POST","/api/v1/auth/register",request(other.email())).statusCode()).isEqualTo(429);
    }
}
