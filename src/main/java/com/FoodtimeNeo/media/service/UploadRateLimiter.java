package com.FoodtimeNeo.media.service;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.UploadLimitsProperties;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** Throttle for private image staging uploads. */
@Service
public class UploadRateLimiter {
    private final StringRedisTemplate redis;
    private final UploadLimitsProperties properties;
    public UploadRateLimiter(StringRedisTemplate redis, UploadLimitsProperties properties) {
        this.redis = redis; this.properties = properties;
    }
    public void check(UserProfile user, String remoteAddress) {
        check(user.id(), remoteAddress);
    }
    public void check(UUID userId, String remoteAddress) {
        try {
            Long retry = RedisAttemptLimiter.check(redis, properties.redisNamespace(), "user", userId.toString(),
                    remoteAddress, properties.userLimit(), properties.ipLimit(), properties.window());
            if (retry == null) { throw unavailable(); }
            if (retry > 0) { throw new UploadRateLimitException(retry); }
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }
    private BusinessException unavailable() {
        return new BusinessException("UPLOAD_UNAVAILABLE", "图片服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
