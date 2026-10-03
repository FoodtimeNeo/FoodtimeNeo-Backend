package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.dto.RegisterRequest;
import com.FoodtimeNeo.auth.dto.RegisterResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.NewUser;
import com.FoodtimeNeo.user.mapper.UserMapper;
import com.FoodtimeNeo.user.service.DefaultDisplayNameGenerator;
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

@Service
@Validated
public class RegistrationService {
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);
    private final UserMapper users;
    private final PasswordEncoder passwords;
    private final DefaultDisplayNameGenerator names;

    public RegistrationService(UserMapper users, PasswordEncoder passwords, DefaultDisplayNameGenerator names) {
        this.users = users;
        this.passwords = passwords;
        this.names = names;
    }

    public RegisterResponse register(@NotNull @Valid RegisterRequest request) {
        try {
            if (users.existsByEmail(request.email())) {
                throw duplicateEmail();
            }
            NewUser user = new NewUser(UUID.randomUUID(), request.email(),
                    passwords.encode(request.password()), names.generate());
            // This single INSERT is atomic; its conflict clause handles concurrent registration.
            if (users.insertRegisteredUser(user) == 0) {
                throw duplicateEmail();
            }
            return new RegisterResponse(user.id(), user.email(), user.displayName(), "user");
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
