package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.dto.RegisterRequest;
import com.FoodtimeNeo.auth.dto.RegisterResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.NewUser;
import com.FoodtimeNeo.user.mapper.UserMapper;
import com.FoodtimeNeo.user.service.DefaultDisplayNameGenerator;
import com.FoodtimeNeo.auth.verification.EmailVerificationService;
import com.FoodtimeNeo.auth.verification.VerificationClaim;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;
import java.time.Clock;

@Service
@Validated
public class RegistrationService {
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);
    private final UserMapper users;
    private final PasswordEncoder passwords;
    private final DefaultDisplayNameGenerator names;
    private final EmailVerificationService verification;
    private final Clock clock;

    public RegistrationService(UserMapper users, PasswordEncoder passwords, DefaultDisplayNameGenerator names,
                               EmailVerificationService verification, Clock clock) {
        this.users = users;
        this.passwords = passwords;
        this.names = names;
        this.verification = verification;
        this.clock = clock;
    }

    public RegisterResponse register(@NotNull @Valid RegisterRequest request) {
        try {
            if (users.existsByEmail(request.email())) {
                throw duplicateEmail();
            }
            VerificationClaim claim;
            try {
                claim = verification.claim(request.email(), request.verificationCode());
            } catch (BusinessException exception) {
                // Another request may have inserted the account and consumed the code since the initial check.
                if (("EMAIL_CODE_INVALID".equals(exception.getCode()) || "REGISTRATION_IN_PROGRESS".equals(exception.getCode()))
                        && users.existsByEmail(request.email())) { throw duplicateEmail(); }
                throw exception;
            }
            boolean registered = false;
            try {
                NewUser user = new NewUser(UUID.randomUUID(), request.email(),
                        passwords.encode(request.password()), names.generate(), clock.instant());
                // PostgreSQL remains the final authority for uniqueness, including concurrent claims.
                if (users.insertRegisteredUser(user) == 0) { throw duplicateEmail(); }
                registered = true;
                return new RegisterResponse(user.id(), user.email(), user.displayName(), "user");
            } finally {
                verification.finish(claim, registered);
            }
        } catch (DataAccessException exception) {
            // SQL exception details can contain account information or hashes. Log only the type.
            LOG.error("Registration persistence failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("REGISTRATION_UNAVAILABLE", "注册服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private BusinessException duplicateEmail() {
        return new BusinessException("EMAIL_ALREADY_REGISTERED", "该邮箱已注册", HttpStatus.CONFLICT);
    }
}
