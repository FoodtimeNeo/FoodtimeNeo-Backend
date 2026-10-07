package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.config.properties.AuthProperties;
import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoginRateLimiter {
    private final StringRedisTemplate redis;
    private final AuthProperties properties;

    public LoginRateLimiter(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void check(String account, String remoteAddress) {
        try {
            Long retry = RedisAttemptLimiter.check(redis, properties.redisNamespace(), "account", account, remoteAddress, properties.loginAccountLimit(), properties.loginIpLimit(), properties.loginWindow());
            if (retry == null) { throw LoginService.unavailable(); }
            if (retry > 0) { throw new LoginRateLimitException(retry); }
        } catch (DataAccessException exception) {
            // Fail closed: a missing limiter must not silently enable unlimited password hashing.
            throw LoginService.unavailable();
        }
    }

    public String key(String dimension, String value) {
        return RedisAttemptLimiter.key(properties.redisNamespace(), dimension, value);
    }
}
