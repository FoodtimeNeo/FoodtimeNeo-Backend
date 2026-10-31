package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.service.RegistrationService;
import com.FoodtimeNeo.auth.verification.EmailVerificationService;
import com.FoodtimeNeo.auth.verification.EmailVerificationStore;
import com.FoodtimeNeo.auth.verification.MailboxVerifier;
import com.FoodtimeNeo.auth.security.DeviceIdentityService;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.config.PasswordConfig;
import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import com.FoodtimeNeo.invitation.service.InvitationService;
import com.FoodtimeNeo.user.entity.NewUser;
import com.FoodtimeNeo.user.mapper.UserMapper;
import com.FoodtimeNeo.user.service.DefaultDisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The former fixed code has exactly the same verification requirements as any other code. */
class RegistrationVerificationTest {
    private final UserMapper users = mock(UserMapper.class);
    private final EmailVerificationStore store = mock(EmailVerificationStore.class);
    private final MailboxVerifier mailboxes = mock(MailboxVerifier.class);
    private final DeviceIdentityService devices = mock(DeviceIdentityService.class);
    private final ObjectProvider<JavaMailSender> mail = mock(ObjectProvider.class);
    private final org.springframework.security.crypto.password.PasswordEncoder passwords = new PasswordConfig().passwordEncoder();

    private MockMvc mvc(boolean enabled) {
        var verification = new EmailVerificationService(store,
                new EmailVerificationProperties(enabled, "sender@example.test", "test-only-secret-at-least-32-characters",
                        "test:email", Duration.ofSeconds(60), Duration.ofMinutes(15), 5, 5, 20),
                mailboxes, mail, Clock.systemUTC());
        var registration = new RegistrationService(users, passwords, new DefaultDisplayNameGenerator(), verification,
                mock(InvitationService.class), Clock.systemUTC());
        return MockMvcBuilders.standaloneSetup(new RegistrationController(registration,
                        mock(com.FoodtimeNeo.auth.service.RegistrationRateLimiter.class), devices))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registration() {
        return post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"123@bjtu.edu.cn","password":"Foodtime2026!","verificationCode":"000000",
                 "role":"superadmin","displayName":"client chosen"}
                """);
    }

    @Test
    void zeroCodeWithoutAnIssuedProofCannotCreateAnAccount() throws Exception {
        mvc(true).perform(registration()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_CODE_INVALID"));
        verify(store).claim(eq("123@bjtu.edu.cn"), eq("000000"), anyString());
        verify(users, never()).insertRegisteredUser(any());
        verifyNoInteractions(mail);
    }

    @Test
    void disabledVerificationFailsClosedForTheFormerTestCode() throws Exception {
        mvc(false).perform(registration()).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EMAIL_VERIFICATION_UNAVAILABLE"));
        verifyNoInteractions(store, mail);
        verify(users, never()).insertRegisteredUser(any());
    }

    @Test
    void zeroIsValidOnlyWhenTheRealStoreGrantsItsProof() throws Exception {
        when(store.claim(eq("123@bjtu.edu.cn"), eq("000000"), anyString())).thenReturn(1L);
        when(users.insertRegisteredUser(any())).thenReturn(1);
        mvc(true).perform(registration()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("user"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
        var inserted = ArgumentCaptor.forClass(NewUser.class);
        verify(users).insertRegisteredUser(inserted.capture());
        assertThat(passwords.matches("Foodtime2026!", inserted.getValue().passwordHash())).isTrue();
        assertThat(inserted.getValue().displayName()).matches("干饭人[0-9]{6}");
        verify(store).finish(argThat(claim -> claim.token() != null), eq(true));
    }
}
