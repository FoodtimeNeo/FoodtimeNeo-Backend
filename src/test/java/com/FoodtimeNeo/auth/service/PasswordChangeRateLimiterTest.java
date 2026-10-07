package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.PasswordChangeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import java.time.Duration;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordChangeRateLimiterTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final PasswordChangeRateLimiter limiter = new PasswordChangeRateLimiter(redis,
            new PasswordChangeProperties(Duration.ofMinutes(15), 5, 50, "test:password"));
    @Test
    void nullRedisResultFailsClosed() {
        assertUnavailable();
    }
    @Test
    void redisFailureFailsClosed() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("private redis"));
        assertUnavailable();
    }
    @Test
    void exceededLimitRetainsRetryTime() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(43L);
        assertThatThrownBy(() -> limiter.check(UUID.randomUUID(), "127.0.0.1"))
                .isInstanceOfSatisfying(PasswordChangeRateLimitException.class, e -> assertThat(e.getRetryAfter()).isEqualTo(43));
    }
    private void assertUnavailable() {
        assertThatThrownBy(() -> limiter.check(UUID.randomUUID(), "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("PASSWORD_CHANGE_UNAVAILABLE"))
                .hasMessageNotContaining("private");
    }
}
