package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.dto.LoginRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.LoginAccount;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.mapper.UserMapper;
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
public class LoginService {
    private static final Logger LOG = LoggerFactory.getLogger(LoginService.class);
    private final UserMapper users;
    private final PasswordEncoder passwords;
    private final String dummyHash;

    public LoginService(UserMapper users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
        // Unknown accounts still perform a password hash comparison to reduce timing differences.
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    public UserProfile authenticate(@NotNull @Valid LoginRequest request) {
        try {
            LoginAccount account = users.findLoginAccount(request.account());
            boolean matched = passwords.matches(request.password(), account == null ? dummyHash : account.passwordHash());
            if (account == null || !matched || !"active".equals(account.status())) {
                throw invalidCredentials();
            }
            if (users.recordSuccessfulLogin(account.id(), account.passwordHash()) != 1) {
                throw invalidCredentials();
            }
            return account.profile();
        } catch (DataAccessException exception) {
            LOG.error("Login persistence failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    public static BusinessException unavailable() {
        return new BusinessException("AUTH_UNAVAILABLE", "登录服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS", "账号或密码错误", HttpStatus.UNAUTHORIZED);
    }
}
