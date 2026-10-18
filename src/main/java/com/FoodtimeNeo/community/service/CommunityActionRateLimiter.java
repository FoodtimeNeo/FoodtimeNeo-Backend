package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.CommunityActionLimitsProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** Lightweight throttle for likes/favorites: high-frequency reads of intent, not content creation. */
@Service
public class CommunityActionRateLimiter {
    private final StringRedisTemplate redis;
    private final CommunityActionLimitsProperties properties;
    public CommunityActionRateLimiter(StringRedisTemplate redis, CommunityActionLimitsProperties properties) {
        this.redis = redis; this.properties = properties;
    }
    public void check(UUID userId, String remoteAddress) {
        try {
            Long retry = RedisAttemptLimiter.check(redis, properties.redisNamespace(), "user", userId.toString(),
                    remoteAddress, properties.userLimit(), properties.ipLimit(), properties.window());
            if (retry == null) { throw unavailable(); }
            if (retry > 0) { throw new CommunityActionRateLimitException(retry); }
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }
    private BusinessException unavailable() {
        return new BusinessException("COMMUNITY_ACTION_UNAVAILABLE", "社区互动服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
