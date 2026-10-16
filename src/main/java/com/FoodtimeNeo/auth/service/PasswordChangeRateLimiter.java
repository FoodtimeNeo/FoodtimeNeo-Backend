package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.config.properties.PasswordChangeProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class PasswordChangeRateLimiter {
    private final StringRedisTemplate redis;
    private final PasswordChangeProperties properties;
    public PasswordChangeRateLimiter(StringRedisTemplate redis, PasswordChangeProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }
    public void check(UUID userId, String remoteAddress) {
        try {
            Long retry = RedisAttemptLimiter.check(redis, properties.redisNamespace(), "user", userId.toString(),
                    remoteAddress, properties.userLimit(), properties.ipLimit(), properties.window());
            if (retry == null) { throw PasswordChangeService.unavailable(); }
            if (retry > 0) { throw new PasswordChangeRateLimitException(retry); }
        } catch (DataAccessException exception) {
            throw PasswordChangeService.unavailable();
        }
    }
    public String key(String dimension, String value) {
        return RedisAttemptLimiter.key(properties.redisNamespace(), dimension, value);
    }
}
