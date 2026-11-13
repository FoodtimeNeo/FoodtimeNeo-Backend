package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import com.FoodtimeNeo.config.properties.RegistrationLimitsProperties;
import com.FoodtimeNeo.config.properties.RegistrationMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationPolicyServiceTest {
    private static RegistrationPolicyService service(RegistrationMode mode, boolean emailEnabled) {
        RegistrationGate gate = new RegistrationGate(new RegistrationLimitsProperties(mode,
                Duration.ofMinutes(15), 10, 10, 1000, "test:registration",
                RegistrationLimitsProperties.DEFAULT_ALLOWED_EMAIL_DOMAINS));
        EmailVerificationProperties email = new EmailVerificationProperties(emailEnabled,
                emailEnabled ? "sender@example.test" : null,
                emailEnabled ? "x".repeat(32) : null,
                "test:email", Duration.ofMinutes(1), Duration.ofMinutes(15), 5, 5, 200);
        return new RegistrationPolicyService(gate, email);
    }

    @Test
    void openModeAllowsOptionalInvitation() {
        var policy = service(RegistrationMode.OPEN, true).current();
        assertThat(policy.mode()).isEqualTo("open");
        assertThat(policy.canSendEmailCode()).isTrue();
        assertThat(policy.invitationRequired()).isFalse();
        assertThat(policy.revision()).isEqualTo("open:email");
    }

    @Test
    void invitationModeRequiresCodeAndStillSendsEmailCodes() {
        var policy = service(RegistrationMode.INVITATION, true).current();
        assertThat(policy.mode()).isEqualTo("invitation");
        assertThat(policy.canSendEmailCode()).isTrue();
        assertThat(policy.invitationRequired()).isTrue();
        assertThat(policy.revision()).isEqualTo("invitation:email");
    }

    @Test
    void closedModeStopsRegistrationAndEmailCodes() {
        var policy = service(RegistrationMode.CLOSED, true).current();
        assertThat(policy.mode()).isEqualTo("closed");
        assertThat(policy.canSendEmailCode()).isFalse();
        assertThat(policy.invitationRequired()).isFalse();
        assertThat(policy.revision()).isEqualTo("closed:no-email");
    }

    @Test
    void disabledEmailVerificationIsReflectedInTheCapability() {
        var policy = service(RegistrationMode.OPEN, false).current();
        assertThat(policy.canSendEmailCode()).isFalse();
        assertThat(policy.revision()).isEqualTo("open:no-email");
    }
}
