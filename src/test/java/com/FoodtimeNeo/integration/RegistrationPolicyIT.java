package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.auth.registration.mode=invitation")
class RegistrationPolicyIT extends IntegrationApiSupport {
    @Test
    void policyIsPublicAndReportsInvitationMode() throws Exception {
        var response = new Browser().call("GET", "/api/v1/auth/registration-policy", null);
        assertThat(response.statusCode()).isEqualTo(200);
        var data = body(response).path("data");
        assertThat(data.path("mode").asString()).isEqualTo("invitation");
        assertThat(data.path("invitationRequired").asBoolean()).isTrue();
        // The dev/test profile leaves email delivery disabled, so the advertised capability is false.
        assertThat(data.path("canSendEmailCode").asBoolean()).isFalse();
        assertThat(data.path("revision").asString()).isEqualTo("invitation:no-email");
    }
}
