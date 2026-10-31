package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.dto.RegisterRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.RegistrationLimitsProperties;
import com.FoodtimeNeo.config.properties.RegistrationMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Applies the deployment-wide registration policy before any work is spent on the request.
 * <p>
 * The check runs before verification, so a closed door never consumes a verification code and an
 * invitation-only door never mints a code that could not be used.
 */
@Component
public class RegistrationGate {
    private final RegistrationLimitsProperties limits;

    public RegistrationGate(RegistrationLimitsProperties limits) {
        this.limits = limits;
    }

    public RegistrationMode mode() {
        return limits.mode();
    }

    /** Blocks both registration and verification-code sending while the door is closed. */
    public void requireAcceptingRequests() {
        if (limits.mode() == RegistrationMode.CLOSED) {
            throw new BusinessException("REGISTRATION_CLOSED", "注册暂未开放，请稍后再试", HttpStatus.FORBIDDEN);
        }
    }

    /** Invitation mode still requires a code; the DTO keeps it optional so open mode stays valid. */
    public void requireInvitation(RegisterRequest request) {
        requireAcceptingRequests();
        if (limits.mode() == RegistrationMode.INVITATION && request.invitationCode() == null) {
            throw new BusinessException("INVITATION_REQUIRED", "当前注册需要邀请码", HttpStatus.BAD_REQUEST);
        }
    }
}
