package com.FoodtimeNeo.auth.verification;

import com.FoodtimeNeo.auth.dto.EmailCodeRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.EmailVerificationProperties;
import com.FoodtimeNeo.config.properties.TestVerificationProperties;
import com.FoodtimeNeo.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
        return service(enabled, false);
    }

    private EmailVerificationService service(boolean enabled, boolean testEnabled) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return new EmailVerificationService(store, new EmailVerificationProperties(enabled, "sender@example.test",
                "test-only-secret-at-least-32-characters", "test:email", Duration.ofSeconds(60),
                Duration.ofMinutes(15), 5, 20), new TestVerificationProperties(testEnabled), provider, users, Clock.systemUTC());
    }

    @Test
    void testCodeWorksWithoutMailOrRedisAndNeverFinalizesARealClaim() {
        var service = service(false, true);
        var claim = service.claim("123@bjtu.edu.cn", "000000");
        assertThat(claim.testVerification()).isTrue();
        assertThat(claim.token()).isNull();
        service.finish(claim, true);
        service.finish(claim, false);
        verifyNoInteractions(store, sender, users);
        assertUnavailable(() -> service.send(new EmailCodeRequest("123@bjtu.edu.cn"), "127.0.0.1"));
        verifyNoInteractions(store, sender, users);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "012345", "00000", "0000000", " 000000", "000000 ", "００００００"})
    void onlyTheExactSixDigitTestCodeCanSkipVerification(String code) {
        assertUnavailable(() -> service(false, true).claim("123@bjtu.edu.cn", code));
        verifyNoInteractions(store, sender);
    }

    @Test
    void disabledTestCodeStillRequiresARealStoredCode() {
        var service = service(true, false);
        assertThatThrownBy(() -> service.claim("123@bjtu.edu.cn", "000000"))
                .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.getCode()).isEqualTo("EMAIL_CODE_INVALID"));
        verify(store).claim(eq("123@bjtu.edu.cn"), eq("000000"), anyString());
        assertUnavailable(() -> service(false, false).claim("123@bjtu.edu.cn", "000000"));
    }

    @Test
    void normalCodesKeepTheirRedisLeaseEvenWhenTestVerificationIsEnabled() {
        when(store.claim(eq("123@bjtu.edu.cn"), eq("012345"), anyString())).thenReturn(1L);
        var service = service(true, true);
        var claim = service.claim("123@bjtu.edu.cn", "012345");
        assertThat(claim.testVerification()).isFalse();
        assertThat(claim.token()).isNotBlank();
        service.finish(claim, true);
        verify(store).finish(claim, true);
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
