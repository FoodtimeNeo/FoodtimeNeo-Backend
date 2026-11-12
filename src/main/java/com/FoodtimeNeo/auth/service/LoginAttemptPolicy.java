package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.config.properties.AuthProperties;
import com.FoodtimeNeo.config.properties.CaptchaProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Rolling per-device login request counter. Every /auth/login request is recorded, including
 * password failures and requests that are themselves rejected for a missing challenge.
 */
@Service
public class LoginAttemptPolicy {
    private static final DefaultRedisScript<Long> RECORD = new DefaultRedisScript<>("""
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', now - window)
            redis.call('ZADD', KEYS[1], now, ARGV[3])
            redis.call('PEXPIRE', KEYS[1], window)
            return redis.call('ZCARD', KEYS[1])
            """, Long.class);
    private final StringRedisTemplate redis;
    private final AuthProperties auth;
    private final CaptchaProperties captcha;
    private final Clock clock;

    public LoginAttemptPolicy(StringRedisTemplate redis, AuthProperties auth,
                              CaptchaProperties captcha, Clock clock) {
        this.redis = redis;
        this.auth = auth;
        this.captcha = captcha;
        this.clock = clock;
    }

    public boolean requiresChallenge(String deviceId) {
        if (!captcha.enabled()) return false;
        String identity = deviceId == null || deviceId.isBlank() ? "unidentified" : deviceId;
        try {
            Long count = redis.execute(RECORD,
                    List.of(RedisAttemptLimiter.key(auth.redisNamespace(), "login-attempt", identity)),
                    Long.toString(clock.millis()),
                    Long.toString(captcha.window().toMillis()),
                    UUID.randomUUID().toString());
            if (count == null) throw LoginService.unavailable();
            return count >= captcha.triggerAfter() + 1L;
        } catch (DataAccessException failure) {
            // Fail closed: Redis failure must not silently disable the challenge.
            throw LoginService.unavailable();
        }
    }
}
