package com.FoodtimeNeo.common.web;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.common.exception.RateLimitedException;
import com.FoodtimeNeo.config.properties.AuthProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Reusable fail-closed Redis rate limiter for high-cost or abuse-prone API endpoints. */
@Service
public class EndpointRateLimiter {
    private final StringRedisTemplate redis;
    private final AuthProperties auth;

    public EndpointRateLimiter(StringRedisTemplate redis, AuthProperties auth) {
        this.redis = redis;
        this.auth = auth;
    }

    public void check(String scope, String identity, String remoteAddress,
                      int identityLimit, int ipLimit, Duration window) {
        String ip = remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
        String keyIdentity = identity == null || identity.isBlank() ? ip : identity;
        try {
            Long retry = RedisAttemptLimiter.check(redis, auth.redisNamespace() + ":endpoint:" + scope,
                    "actor", keyIdentity, ip, identityLimit, ipLimit, window);
            if (retry == null) {
                throw unavailable();
            }
            if (retry > 0) {
                throw new EndpointRateLimited(retry);
            }
        } catch (DataAccessException failure) {
            throw unavailable();
        }
    }

    private static BusinessException unavailable() {
        return new BusinessException("RATE_LIMIT_UNAVAILABLE", "服务暂时不可用，请稍后重试",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static final class EndpointRateLimited extends RateLimitedException {
        EndpointRateLimited(long retryAfter) {
            super("RATE_LIMITED", "请求过于频繁，请稍后重试", retryAfter);
        }
    }
}
