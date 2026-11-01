package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.dto.RegisterRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.RegistrationLimitsProperties;
import com.FoodtimeNeo.config.properties.RegistrationMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationGateTest {
    private static RegisterRequest request(String invitationCode) {
        return new RegisterRequest("123@bjtu.edu.cn", "Abc12345", "012345", invitationCode);
    }

    private static RegistrationGate gate(RegistrationMode mode) {
        return new RegistrationGate(new RegistrationLimitsProperties(mode, Duration.ofMinutes(15),
                10, 10, 1000, "test:registration"));
    }

    @Test
    void openAcceptsRegistrationWithoutAnInvitation() {
        RegistrationGate gate = gate(RegistrationMode.OPEN);
        assertThatCode(() -> gate.requireInvitation(request(null))).doesNotThrowAnyException();
    }

    @Test
    void invitationRequiresACode() {
        RegistrationGate gate = gate(RegistrationMode.INVITATION);
        assertThatThrownBy(() -> gate.requireInvitation(request(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("INVITATION_REQUIRED");
        assertThatCode(() -> gate.requireInvitation(request("ABCD2345EFGH"))).doesNotThrowAnyException();
    }

    @Test
    void closedRejectsBothRegistrationAndCodeSending() {
        RegistrationGate gate = gate(RegistrationMode.CLOSED);
        assertThatThrownBy(gate::requireAcceptingRequests)
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("REGISTRATION_CLOSED");
        // The invitation path must not bypass a closed door either.
        assertThatThrownBy(() -> gate.requireInvitation(request("ABCD2345EFGH")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("REGISTRATION_CLOSED");
    }

    @Test
    void closedIsReportedAsForbidden() {
        BusinessException error = org.assertj.core.api.Assertions.catchThrowableOfType(
                BusinessException.class, () -> gate(RegistrationMode.CLOSED).requireAcceptingRequests());
        assertThat(error.getStatus().value()).isEqualTo(403);
    }
}
