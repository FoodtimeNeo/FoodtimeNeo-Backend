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
import java.util.Locale;
import java.util.UUID;

/**
 * Rolling per-device and per-account login request counter. Every /auth/login request is recorded,
 * including password failures and requests that are themselves rejected for a missing challenge.
 */
@Service
public class LoginAttemptPolicy {
    private static final DefaultRedisScript<Long> RECORD = new DefaultRedisScript<>("""
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local peak = 0
            for i = 1, #KEYS do
              redis.call('ZREMRANGEBYSCORE', KEYS[i], '-inf', now - window)
              redis.call('ZADD', KEYS[i], now, ARGV[3])
              redis.call('PEXPIRE', KEYS[i], window)
              local count = redis.call('ZCARD', KEYS[i])
              if count > peak then peak = count end
            end
            return peak
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
        return requiresChallenge(deviceId, null);
    }

    public boolean requiresChallenge(String deviceId, String account) {
        if (!captcha.enabled()) return false;
        String identity = deviceId == null || deviceId.isBlank() ? "unidentified" : deviceId;
        String normalizedAccount = account == null ? "" : account.strip().toLowerCase(Locale.ROOT);
        List<String> keys = normalizedAccount.isEmpty()
                ? List.of(RedisAttemptLimiter.key(auth.redisNamespace(), "login-attempt", identity))
                : List.of(RedisAttemptLimiter.key(auth.redisNamespace(), "login-attempt", identity),
                        RedisAttemptLimiter.key(auth.redisNamespace(), "login-attempt-account", normalizedAccount));
        try {
            Long count = redis.execute(RECORD,
                    keys,
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
