package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.AuthProperties;
import com.FoodtimeNeo.config.properties.CaptchaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginAttemptPolicyTest {
    private static final Instant NOW = Instant.parse("2026-11-12T00:00:00Z");

    private static AuthProperties auth() {
        return new AuthProperties(Duration.ofDays(7), Duration.ofMinutes(45), false, Duration.ofMinutes(15), 10, 1000, "test:auth");
    }

    private static CaptchaProperties captcha(boolean enabled) {
        return new CaptchaProperties(enabled, Duration.ofMinutes(15), 1, Duration.ofMinutes(2),
                Duration.ofMinutes(15), 30, 300, "test:captcha", "x".repeat(32));
    }

    @Test
    @SuppressWarnings("unchecked")
    void secondRequestInTheRollingWindowRequiresChallenge() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L, 2L, 3L);
        LoginAttemptPolicy policy = new LoginAttemptPolicy(redis, auth(), captcha(true),
                Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(policy.requiresChallenge("device-1")).isFalse();
        assertThat(policy.requiresChallenge("device-1")).isTrue();
        assertThat(policy.requiresChallenge("device-1")).isTrue();
    }

    @Test
    void disabledCaptchaNeverRequiresAChallenge() {
        LoginAttemptPolicy policy = new LoginAttemptPolicy(mock(StringRedisTemplate.class), auth(),
                captcha(false), Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(policy.requiresChallenge("device-1")).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisFailureFailsClosed() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("down"));
        LoginAttemptPolicy policy = new LoginAttemptPolicy(redis, auth(), captcha(true),
                Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> policy.requiresChallenge("device-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("AUTH_UNAVAILABLE");
    }
}
