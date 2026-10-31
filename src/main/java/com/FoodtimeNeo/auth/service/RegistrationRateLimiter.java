package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.auth.verification.EmailVerificationService;
import com.FoodtimeNeo.common.exception.RateLimitedException;
import com.FoodtimeNeo.config.properties.RegistrationLimitsProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Count both code requests and registration attempts before looking up account existence. */
@Service
public class RegistrationRateLimiter {
    private final StringRedisTemplate redis;
    private final RegistrationLimitsProperties properties;
    public RegistrationRateLimiter(StringRedisTemplate redis, RegistrationLimitsProperties properties) {
        this.redis = redis; this.properties = properties;
    }
    public void check(String email, String remoteAddress) {
        try {
            Long retry = RedisAttemptLimiter.check(redis, properties.redisNamespace(), "account", email, remoteAddress,
                    properties.accountLimit(), properties.ipLimit(), properties.window());
            if (retry == null) throw EmailVerificationService.unavailable();
            if (retry > 0) throw new RegistrationLimited(retry);
        } catch (DataAccessException failure) { throw EmailVerificationService.unavailable(); }
    }
    private static class RegistrationLimited extends RateLimitedException {
        RegistrationLimited(long retry) { super("REGISTRATION_RATE_LIMITED", "注册尝试过于频繁，请稍后重试", retry); }
    }
}
