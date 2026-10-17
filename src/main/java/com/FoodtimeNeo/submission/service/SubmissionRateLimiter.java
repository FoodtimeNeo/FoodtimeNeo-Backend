package com.FoodtimeNeo.submission.service;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.config.properties.SubmissionLimitsProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.FoodtimeNeo.common.exception.BusinessException;
import java.util.UUID;

/** Shared throttle for review and catalog/image submission creation. */
@Service
public class SubmissionRateLimiter {
    private final StringRedisTemplate redis;
    private final SubmissionLimitsProperties properties;
    public SubmissionRateLimiter(StringRedisTemplate redis, SubmissionLimitsProperties properties) {
        this.redis = redis; this.properties = properties;
    }
    public void check(UUID userId, String remoteAddress) {
        try {
            Long retry = RedisAttemptLimiter.check(redis, properties.redisNamespace(), "user", userId.toString(),
                    remoteAddress, properties.userLimit(), properties.ipLimit(), properties.window());
            if (retry == null) { throw unavailable(); }
            if (retry > 0) { throw new SubmissionRateLimitException(retry); }
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }
    private BusinessException unavailable() {
        return new BusinessException("SUBMISSION_UNAVAILABLE", "投稿服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
