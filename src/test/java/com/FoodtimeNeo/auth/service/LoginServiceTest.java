package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.dto.LoginRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.LoginAccount;
import com.FoodtimeNeo.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginServiceTest {
    private final UserMapper users = mock(UserMapper.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private LoginService service;
    private final LoginAccount account = new LoginAccount(UUID.randomUUID(), "123@bjtu.edu.cn", "stored-hash",
            "干饭人123456", "user", "active", Instant.parse("2026-10-01T00:00:00Z"));

    @BeforeEach
    void setUp() {
        when(passwords.encode(anyString())).thenReturn("dummy-hash");
        service = new LoginService(users, passwords);
    }

    @Test
    void normalizedAccountAndExactPasswordProduceCurrentProfileAndRecordLogin() {
        when(users.findLoginAccount(account.email())).thenReturn(account);
        when(passwords.matches("Abc12345!", account.passwordHash())).thenReturn(true);
        when(users.recordSuccessfulLogin(account.id(), account.passwordHash())).thenReturn(1);
        assertThat(service.authenticate(new LoginRequest(" 123@BJTU.EDU.CN ", "Abc12345!"))).isEqualTo(account.profile());
        verify(passwords).matches("Abc12345!", account.passwordHash());
        assertThat(new LoginRequest(account.email(), "Abc12345!").toString()).doesNotContain(account.email(), "Abc12345!");
        assertThat(account.toString()).doesNotContain(account.passwordHash());
    }

    @Test
    void unknownAccountsStillPerformAnArgon2ComparisonAndReturnGeneric401() {
        assertBadCredentials(new LoginRequest("999@bjtu.edu.cn", "Abc12345"));
        verify(passwords).matches("Abc12345", "dummy-hash");
        verify(users, never()).recordSuccessfulLogin(any(), anyString());
    }

    @Test
    void wrongPasswordDoesNotUpdateTheLoginTimestamp() {
        when(users.findLoginAccount(account.email())).thenReturn(account);
        assertBadCredentials(new LoginRequest(account.email(), "Wrong123"));
        verify(users, never()).recordSuccessfulLogin(any(), anyString());
    }

    @Test
    void disabledAccountsReturnTheSameErrorEvenWithCorrectPassword() {
        var disabled = new LoginAccount(account.id(), account.email(), account.passwordHash(), account.displayName(),
                account.role(), "disabled", account.passwordChangedAt());
        when(users.findLoginAccount(account.email())).thenReturn(disabled);
        when(passwords.matches("Abc12345", account.passwordHash())).thenReturn(true);
        assertBadCredentials(new LoginRequest(account.email(), "Abc12345"));
        verify(users, never()).recordSuccessfulLogin(any(), anyString());
    }

    @Test
    void concurrentPasswordChangeCannotGrantAnAuthenticatedSession() {
        when(users.findLoginAccount(account.email())).thenReturn(account);
        when(passwords.matches("Abc12345", account.passwordHash())).thenReturn(true);
        when(users.recordSuccessfulLogin(account.id(), account.passwordHash())).thenReturn(0);
        assertBadCredentials(new LoginRequest(account.email(), "Abc12345"));
    }

    @Test
    void databaseFailureReturns503WithoutInternalDetails() {
        when(users.findLoginAccount(anyString())).thenThrow(new DataAccessResourceFailureException("private SQL data"));
        assertThatThrownBy(() -> service.authenticate(new LoginRequest(account.email(), "Abc12345")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(503);
                    assertThat(exception.getCode()).isEqualTo("AUTH_UNAVAILABLE");
                    assertThat(exception.getMessage()).doesNotContain("private SQL data");
                });
    }

    private void assertBadCredentials(LoginRequest request) {
        assertThatThrownBy(() -> service.authenticate(request)).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getStatus().value()).isEqualTo(401);
            assertThat(error.getCode()).isEqualTo("INVALID_CREDENTIALS");
            assertThat(error.getMessage()).isEqualTo("账号或密码错误");
        });
    }
}
