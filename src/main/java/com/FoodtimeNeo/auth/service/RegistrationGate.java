package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.dto.RegisterRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.RegistrationLimitsProperties;
import com.FoodtimeNeo.config.properties.RegistrationMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Applies the deployment-wide registration policy before any work is spent on the request.
 * <p>
 * The check runs before verification, so a closed door never consumes a verification code and an
 * invitation-only door never mints a code that could not be used.
 */
@Component
public class RegistrationGate {
    private final RegistrationLimitsProperties limits;
    private final Set<String> allowedEmailDomains;

    public RegistrationGate(RegistrationLimitsProperties limits) {
        this.limits = limits;
        this.allowedEmailDomains = Set.copyOf(limits.allowedEmailDomains());
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

    /** 只允许主流邮箱；格式错误交给 DTO 校验，这里不重复报错。 */
    public void requireAllowedEmail(String email) {
        if (email == null) return;
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) return;
        String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
        for (String allowed : allowedEmailDomains) {
            if (domain.equals(allowed) || domain.endsWith("." + allowed)) return;
        }
        throw new BusinessException("EMAIL_DOMAIN_NOT_ALLOWED",
                "请使用主流邮箱注册，如 QQ、163、126、Gmail、Outlook 或学校邮箱", HttpStatus.BAD_REQUEST);
    }

    /** Invitation mode still requires a code; the DTO keeps it optional so open mode stays valid. */
    public void requireInvitation(RegisterRequest request) {
        requireAcceptingRequests();
        requireAllowedEmail(request.email());
        if (limits.mode() == RegistrationMode.INVITATION && request.invitationCode() == null) {
            throw new BusinessException("INVITATION_REQUIRED", "当前注册需要邀请码", HttpStatus.BAD_REQUEST);
        }
    }
}
