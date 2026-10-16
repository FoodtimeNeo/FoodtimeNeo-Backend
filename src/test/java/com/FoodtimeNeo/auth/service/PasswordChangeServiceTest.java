package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.dto.ChangePasswordRequest;
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

class PasswordChangeServiceTest {
    private final UserMapper users = mock(UserMapper.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final PasswordChangeService service = new PasswordChangeService(users, passwords);
    private final LoginAccount account = new LoginAccount(UUID.randomUUID(), "123@bjtu.edu.cn", "old-hash",
            "干饭人123456", "user", "active", Instant.parse("2026-10-01T00:00:00Z"));
    private final ChangePasswordRequest request = new ChangePasswordRequest("Old12345!", "New12345!");

    @BeforeEach
    void setUp() { when(users.findLoginAccountById(account.id())).thenReturn(account); }

    @Test
    void validatesOldPasswordAndConditionallyWritesOnlyTheNewHash() {
        when(passwords.matches(request.oldPassword(), account.passwordHash())).thenReturn(true);
        when(passwords.encode(request.newPassword())).thenReturn("new-hash");
        when(users.changePassword(account.id(), "old-hash", account.passwordChangedAt(), "new-hash")).thenReturn(1);
        service.change(account.profile(), request);
        var ordered = inOrder(passwords, users);
        ordered.verify(users).findLoginAccountById(account.id());
        ordered.verify(passwords).matches("Old12345!", "old-hash");
        ordered.verify(passwords).encode("New12345!");
        ordered.verify(users).changePassword(account.id(), "old-hash", account.passwordChangedAt(), "new-hash");
    }

    @Test
    void wrongOldPasswordDoesNotHashOrWrite() {
        assertCode("OLD_PASSWORD_INCORRECT", () -> service.change(account.profile(), request));
        verify(passwords, never()).encode(anyString());
        verify(users, never()).changePassword(any(), any(), any(), any());
    }

    @Test
    void unchangedPasswordDoesNotHashOrWrite() {
        when(passwords.matches(request.oldPassword(), "old-hash")).thenReturn(true);
        assertCode("PASSWORD_UNCHANGED", () -> service.change(account.profile(),
                new ChangePasswordRequest(request.oldPassword(), request.oldPassword())));
        verify(passwords, never()).encode(anyString());
    }

    @Test
    void missingAccountCannotChangePassword() {
        when(users.findLoginAccountById(account.id())).thenReturn(null);
        assertCode("UNAUTHENTICATED", () -> service.change(account.profile(), request));
        verifyNoInteractions(passwords);
    }

    @Test
    void disabledAccountCannotChangePassword() {
        when(users.findLoginAccountById(account.id())).thenReturn(new LoginAccount(account.id(), account.email(),
                account.passwordHash(), account.displayName(), account.role(), "disabled", account.passwordChangedAt()));
        assertCode("UNAUTHENTICATED", () -> service.change(account.profile(), request));
        verifyNoInteractions(passwords);
    }

    @Test
    void staleAuthenticatedProfileIsRejectedBeforeCheckingOldPassword() {
        when(users.findLoginAccountById(account.id())).thenReturn(new LoginAccount(account.id(), account.email(),
                account.passwordHash(), account.displayName(), account.role(), "active", account.passwordChangedAt().plusSeconds(1)));
        assertCode("UNAUTHENTICATED", () -> service.change(account.profile(), request));
        verifyNoInteractions(passwords);
    }

    @Test
    void concurrentDatabaseChangeReturnsConflict() {
        when(passwords.matches(request.oldPassword(), "old-hash")).thenReturn(true);
        when(passwords.encode(request.newPassword())).thenReturn("new-hash");
        assertCode("PASSWORD_CHANGE_CONFLICT", () -> service.change(account.profile(), request));
    }

    @Test
    void databaseFailureDoesNotExposePrivateDetails() {
        when(users.findLoginAccountById(account.id())).thenThrow(new DataAccessResourceFailureException("private-sql"));
        assertCode("PASSWORD_CHANGE_UNAVAILABLE", () -> service.change(account.profile(), request));
        verifyNoInteractions(passwords);
    }

    private void assertCode(String code, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getCode()).isEqualTo(code)).hasMessageNotContaining("private-sql");
    }
}
