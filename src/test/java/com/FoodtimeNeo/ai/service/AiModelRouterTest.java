package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.AiTriageProperties;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiModelRouterTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final HashOperations<String, String, String> hash = mock(HashOperations.class);
    private final AiTriageProperties properties = new AiTriageProperties(false, "openai", "https://api.example.com",
            "secret", "env-primary", "env-escalation", "env-fallback", 0.9, "env-photo", 0.95, 0.9,
            "foodtime:ai:test", Duration.ofSeconds(30), 10, Duration.ofMinutes(5), Duration.ofMinutes(1));
    private final AiModelRouter router = new AiModelRouter(properties, redis);

    {
        doReturn(hash).when(redis).opsForHash();
    }

    @Test
    void fallsBackToEnvWhenRedisHasNoOverride() {
        when(hash.get("foodtime:ai:test", "primary-model")).thenReturn(null);
        assertThat(router.primary()).isEqualTo("env-primary");
    }

    @Test
    void blankOverrideIsIgnored() {
        when(hash.get("foodtime:ai:test", "photo-model")).thenReturn("  ");
        assertThat(router.photo()).isEqualTo("env-photo");
    }

    @Test
    void redisOverrideWinsOverEnv() {
        when(hash.get("foodtime:ai:test", "fallback-model")).thenReturn(" pinned-fallback ");
        assertThat(router.fallback()).isEqualTo("pinned-fallback");
    }

    @Test
    void redisFailureFailsOpenToEnv() {
        when(hash.get("foodtime:ai:test", "escalation-model"))
                .thenThrow(new RedisConnectionFailureException("private redis"));
        assertThat(router.escalation()).isEqualTo("env-escalation");
    }

    @Test
    void snapshotMarksSourcePerSlot() {
        when(hash.get("foodtime:ai:test", "primary-model")).thenReturn("pinned-primary");
        when(hash.get("foodtime:ai:test", "escalation-model")).thenReturn(null);
        var rows = router.snapshot();
        assertThat(rows.get("primary-model")).containsEntry("model", "pinned-primary").containsEntry("source", "redis");
        assertThat(rows.get("escalation-model")).containsEntry("model", "env-escalation").containsEntry("source", "env");
    }

    @Test
    void pinRequiresSuperadminAndWritesHash() {
        var user = actor("admin");
        assertThatThrownBy(() -> router.pin(user, "primary-model", "m", null))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("ACCESS_DENIED"));
        assertThatThrownBy(() -> router.pin(actor("superadmin"), "nope", "m", null))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> router.pin(actor("superadmin"), "primary-model", "has space", null))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("VALIDATION_ERROR"));

        router.pin(actor("superadmin"), "primary-model", "new-model", "monitor promotion");
        verify(hash).putAll(eq("foodtime:ai:test"), any());
    }

    private static UserProfile actor(String role) {
        return new UserProfile(UUID.randomUUID(), "op@example.com", "op", role, "active", Instant.now());
    }
}
