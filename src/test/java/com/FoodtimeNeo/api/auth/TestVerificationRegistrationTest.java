package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.service.RegistrationService;
import com.FoodtimeNeo.auth.verification.EmailVerificationService;
import com.FoodtimeNeo.auth.verification.EmailVerificationStore;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.config.PasswordConfig;
import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import com.FoodtimeNeo.config.properties.TestVerificationProperties;
import com.FoodtimeNeo.user.entity.NewUser;
import com.FoodtimeNeo.user.mapper.UserMapper;
import com.FoodtimeNeo.user.service.DefaultDisplayNameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercise the real registration and verification services through HTTP parameter validation. */
class TestVerificationRegistrationTest {
    private final UserMapper users = mock(UserMapper.class);
    private final EmailVerificationStore store = mock(EmailVerificationStore.class);
    private final ObjectProvider<JavaMailSender> mail = mock(ObjectProvider.class);
    private final PasswordEncoder passwords = new PasswordConfig().passwordEncoder();
    private final Instant now = Instant.parse("2026-10-15T00:00:00Z");
    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        var verification = new EmailVerificationService(store,
                new EmailVerificationProperties(false, "", "", "test:email", Duration.ofSeconds(60),
                        Duration.ofMinutes(15), 5, 20),
                new TestVerificationProperties(true), mail, users, clock);
        var registration = new RegistrationService(users, passwords, new DefaultDisplayNameGenerator(), verification, clock);
        mvc = MockMvcBuilders.standaloneSetup(new RegistrationController(registration))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void testCodeCreatesAnOrdinaryAccountWithARealPasswordHashWithoutSendingMail() throws Exception {
        when(users.insertRegisteredUser(any())).thenReturn(1);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(" 00123@BJTU.EDU.CN ", "Foodtime2026!", "000000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("00123@bjtu.edu.cn"))
                .andExpect(jsonPath("$.data.role").value("user"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
        var inserted = ArgumentCaptor.forClass(NewUser.class);
        verify(users).insertRegisteredUser(inserted.capture());
        NewUser user = inserted.getValue();
        assertThat(user.displayName()).matches("干饭人[0-9]{6}");
        assertThat(user.emailVerifiedAt()).isEqualTo(now);
        assertThat(user.passwordHash()).startsWith("{argon2id}$argon2id$");
        assertThat(passwords.matches("Foodtime2026!", user.passwordHash())).isTrue();
        assertThat(passwords.matches("000000", user.passwordHash())).isFalse();
        verifyNoInteractions(store, mail);
    }

    @ParameterizedTest
    @CsvSource({"abc@bjtu.edu.cn,Foodtime2026!,000000", "123@bjtu.edu.cn,000000,000000",
            "123@bjtu.edu.cn,Foodtime2026!,00000", "123@bjtu.edu.cn,Foodtime2026!,0000000"})
    void testModeStillRejectsInvalidEmailPasswordAndCodeFormat(String email, String password, String code) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, password, code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(users, store, mail);
    }

    @Test
    void testCodeCannotReplaceAnExistingAccount() throws Exception {
        when(users.existsByEmail("123@bjtu.edu.cn")).thenReturn(true);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("123@bjtu.edu.cn", "Foodtime2026!", "000000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        verify(users, never()).insertRegisteredUser(any());
        verifyNoInteractions(store, mail);
    }

    @Test
    void aConcurrentTestRegistrationStillUsesTheDatabaseUniquenessCheck() throws Exception {
        when(users.insertRegisteredUser(any())).thenReturn(0);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("123@bjtu.edu.cn", "Foodtime2026!", "000000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
        verifyNoInteractions(store, mail);
    }

    private String body(String email, String password, String code) {
        // Extra privileges must remain ignored in test mode too.
        return json.writeValueAsString(Map.of("email", email, "password", password, "verificationCode", code,
                "role", "superadmin", "displayName", "client chosen", "status", "disabled"));
    }
}
