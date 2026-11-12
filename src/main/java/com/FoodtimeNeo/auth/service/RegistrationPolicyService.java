package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.dto.RegistrationPolicyResponse;
import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import com.FoodtimeNeo.config.properties.RegistrationMode;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Projects the deployment-wide registration gate into the small, stable capability set that both
 * clients need. It deliberately does not expose limits, invitation inventory or account state.
 */
@Service
public class RegistrationPolicyService {
    private final RegistrationGate gate;
    private final EmailVerificationProperties emailVerification;

    public RegistrationPolicyService(RegistrationGate gate, EmailVerificationProperties emailVerification) {
        this.gate = gate;
        this.emailVerification = emailVerification;
    }

    public RegistrationPolicyResponse current() {
        RegistrationMode mode = gate.mode();
        boolean canSendEmailCode = mode != RegistrationMode.CLOSED && emailVerification.enabled();
        String modeName = mode.name().toLowerCase(Locale.ROOT);
        // Config changes require a restart today, so this value changes with the effective capability.
        // It is intentionally not a hash of internal limits: clients only need a change signal.
        String revision = modeName + ":" + (canSendEmailCode ? "email" : "no-email");
        return new RegistrationPolicyResponse(modeName, canSendEmailCode,
                mode == RegistrationMode.INVITATION, revision);
    }
}
