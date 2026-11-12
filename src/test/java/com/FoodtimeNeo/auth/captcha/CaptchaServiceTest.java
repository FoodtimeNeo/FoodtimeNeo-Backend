package com.FoodtimeNeo.auth.captcha;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.CaptchaProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaptchaServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-12T00:00:00Z");
    private static final byte[] IMAGE = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47};

    private static CaptchaProperties properties(boolean enabled) {
        return new CaptchaProperties(enabled, Duration.ofMinutes(15), 1, Duration.ofMinutes(2),
                Duration.ofMinutes(15), 30, 300, "test:captcha", "x".repeat(32));
    }

    @Test
    void issueStoresTheAnswerAndReturnsOnlyTheImage() {
        ChallengeProvider provider = mock(ChallengeProvider.class);
        ChallengeStore store = mock(ChallengeStore.class);
        when(provider.generate()).thenReturn(new GeneratedChallenge("2 + 2 = ?", 4, IMAGE));
        CaptchaService service = new CaptchaService(provider, store, properties(true),
                Clock.fixed(NOW, ZoneOffset.UTC));

        var issued = service.issue("device-1");
        assertThat(issued.challengeId()).matches("[0-9a-f-]{36}");
        assertThat(issued.image()).isEqualTo(IMAGE);
        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));
        verify(store).save(eq(issued.challengeId()), eq(4), eq("device-1"), eq(Duration.ofMinutes(2)));
    }

    @Test
    void verifyDelegatesToTheOneTimeStore() {
        ChallengeProvider provider = mock(ChallengeProvider.class);
        ChallengeStore store = mock(ChallengeStore.class);
        when(store.consume("challenge", "4", "device-1")).thenReturn(true);
        CaptchaService service = new CaptchaService(provider, store, properties(true),
                Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(service.verify("challenge", "4", "device-1")).isTrue();
        when(store.consume("challenge", "4", "device-1")).thenReturn(false);
        assertThat(service.verify("challenge", "4", "device-1")).isFalse();
    }

    @Test
    void disabledCaptchaCannotIssueOrVerify() {
        CaptchaService service = new CaptchaService(mock(ChallengeProvider.class), mock(ChallengeStore.class),
                properties(false), Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> service.issue("device-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("CAPTCHA_DISABLED");
        assertThat(service.verify("challenge", "4", "device-1")).isFalse();
    }
}
