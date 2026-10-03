package com.FoodtimeNeo.auth;

import com.FoodtimeNeo.auth.dto.RegisterRequest;
import com.FoodtimeNeo.auth.service.RegistrationService;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.PasswordConfig;
import com.FoodtimeNeo.user.entity.NewUser;
import com.FoodtimeNeo.user.mapper.UserMapper;
import com.FoodtimeNeo.user.service.DefaultDisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {
    @Test
    void persistenceReceivesOnlyTheHashAndServerGeneratedDefaults() {
        UserMapper users = mock(UserMapper.class);
        PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
        when(users.insertRegisteredUser(any())).thenReturn(1);
        RegistrationService service = new RegistrationService(users, encoder, new DefaultDisplayNameGenerator());
        var response = service.register(new RegisterRequest("123@bjtu.edu.cn", "Abc12345"));
        ArgumentCaptor<NewUser> inserted = ArgumentCaptor.forClass(NewUser.class);
        verify(users).insertRegisteredUser(inserted.capture());
        NewUser user = inserted.getValue();
        assertThat(user.passwordHash()).isNotEqualTo("Abc12345");
        assertThat(encoder.matches("Abc12345", user.passwordHash())).isTrue();
        assertThat(response.id()).isEqualTo(user.id());
        assertThat(response.displayName()).matches("干饭人[0-9]{6}");
        assertThat(response.role()).isEqualTo("user");
        assertThat(new RegisterRequest("123@bjtu.edu.cn", "Abc12345").toString()).doesNotContain("Abc12345", "123@bjtu.edu.cn");
        assertThat(user.toString()).doesNotContain(user.passwordHash(), "123@bjtu.edu.cn");
    }

    @Test
    void existingEmailIsRejectedBeforePasswordHashing() {
        UserMapper users = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.existsByEmail("123@bjtu.edu.cn")).thenReturn(true);
        var service = new RegistrationService(users, encoder, new DefaultDisplayNameGenerator());
        assertThatThrownBy(() -> service.register(new RegisterRequest("123@bjtu.edu.cn", "Abc12345")))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("EMAIL_ALREADY_REGISTERED");
                    assertThat(error.getStatus().value()).isEqualTo(409);
                });
        verifyNoInteractions(encoder);
        verify(users, never()).insertRegisteredUser(any());
    }

    @Test
    void aConcurrentEmailConflictIsAlsoReportedAs409() {
        UserMapper users = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("salted-hash");
        when(users.insertRegisteredUser(any())).thenReturn(0);
        var service = new RegistrationService(users, encoder, new DefaultDisplayNameGenerator());
        assertThatThrownBy(() -> service.register(new RegisterRequest("123@bjtu.edu.cn", "Abc12345")))
                .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.getCode()).isEqualTo("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void databaseFailuresAreReportedWithoutExposingSqlOrCredentials() {
        UserMapper users = mock(UserMapper.class);
        when(users.existsByEmail(any())).thenThrow(new DataAccessResourceFailureException("private SQL details"));
        var service = new RegistrationService(users, mock(PasswordEncoder.class), new DefaultDisplayNameGenerator());
        assertThatThrownBy(() -> service.register(new RegisterRequest("123@bjtu.edu.cn", "Abc12345")))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("REGISTRATION_UNAVAILABLE");
                    assertThat(error.getStatus().value()).isEqualTo(503);
                    assertThat(error.getMessage()).doesNotContain("SQL", "Abc12345");
                });
    }
}
