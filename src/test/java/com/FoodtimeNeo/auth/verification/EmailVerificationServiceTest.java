package com.FoodtimeNeo.auth.verification;

import com.FoodtimeNeo.auth.dto.EmailCodeRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import com.FoodtimeNeo.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.Clock;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {
    private final EmailVerificationStore store = mock(EmailVerificationStore.class);
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final UserMapper users = mock(UserMapper.class);

    private EmailVerificationService service(boolean enabled) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return new EmailVerificationService(store, new EmailVerificationProperties(enabled, "sender@example.test",
                "test-only-secret-at-least-32-characters", "test:email", Duration.ofSeconds(60),
                Duration.ofMinutes(15), 5, 20), provider, users, Clock.systemUTC());
    }

    @Test
    void disabledFeatureCannotSendOrBypassRegistrationVerification() {
        var service = service(false);
        assertUnavailable(() -> service.send(new EmailCodeRequest("123@bjtu.edu.cn"), "127.0.0.1"));
        assertUnavailable(() -> service.claim("123@bjtu.edu.cn", "012345"));
        verifyNoInteractions(users, store, sender);
    }

    @Test
    void redisFailureCannotSendMailWithoutAbuseProtection() {
        when(store.reserveSend(anyString(), anyString(), anyString())).thenThrow(new RedisConnectionFailureException("private Redis data"));
        assertUnavailable(() -> service(true).send(new EmailCodeRequest("123@bjtu.edu.cn"), "127.0.0.1"));
        verifyNoInteractions(sender);
    }

    @Test
    void smtpFailureIsSafeAndNeverPublishesTheGeneratedCode() {
        doThrow(new MailAuthenticationException("private SMTP password")).when(sender).send(any(SimpleMailMessage.class));
        assertUnavailable(() -> service(true).send(new EmailCodeRequest("123@bjtu.edu.cn"), "127.0.0.1"));
        verify(store, never()).publish(anyString(), anyString(), anyString());
        verify(store).cancelSend(eq("123@bjtu.edu.cn"), anyString());
    }

    @Test
    void anAlreadyRegisteredAccountNeverReceivesVerificationMail() {
        when(users.existsByEmail("123@bjtu.edu.cn")).thenReturn(true);
        assertThatThrownBy(() -> service(true).send(new EmailCodeRequest("123@bjtu.edu.cn"), "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.getCode()).isEqualTo("EMAIL_ALREADY_REGISTERED"));
        verifyNoInteractions(store, sender);
    }

    private void assertUnavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getStatus().value()).isEqualTo(503);
            assertThat(error.getCode()).isEqualTo("EMAIL_VERIFICATION_UNAVAILABLE");
            assertThat(error.getMessage()).doesNotContain("private SMTP password", "private Redis data");
        });
    }
}
