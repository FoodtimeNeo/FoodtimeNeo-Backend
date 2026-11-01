package com.FoodtimeNeo.follow.service;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.common.exception.RateLimitedException;
import com.FoodtimeNeo.config.properties.FollowLimitsProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Throttles follow and unfollow so the relation table cannot be churned at script speed. */
@Service
public class FollowRateLimiter {
    private final StringRedisTemplate redis;
    private final FollowLimitsProperties properties;

    public FollowRateLimiter(StringRedisTemplate redis, FollowLimitsProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void check(UUID userId, String remoteAddress) {
        try {
            Long retry = RedisAttemptLimiter.check(redis, properties.redisNamespace(), "user",
                    userId.toString(), remoteAddress, properties.userLimit(), properties.ipLimit(),
                    properties.window());
            if (retry == null) {
                throw unavailable();
            }
            if (retry > 0) {
                throw new FollowLimited(retry);
            }
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    private static BusinessException unavailable() {
        return new BusinessException("FOLLOW_UNAVAILABLE", "关注服务暂时不可用，请稍后重试",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static final class FollowLimited extends RateLimitedException {
        private FollowLimited(long retry) {
            super("FOLLOW_RATE_LIMITED", "操作过于频繁，请稍后再试", retry);
        }
    }
}
