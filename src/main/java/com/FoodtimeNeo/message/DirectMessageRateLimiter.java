package com.FoodtimeNeo.message;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.DirectMessageLimitsProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** 私信发送限流：内容型写操作，按用户与来源 IP 双重计数。 */
@Service
public class DirectMessageRateLimiter {
    private final StringRedisTemplate redis;
    private final DirectMessageLimitsProperties properties;

    public DirectMessageRateLimiter(StringRedisTemplate redis, DirectMessageLimitsProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void check(UUID userId, String remoteAddress) {
        try {
            Long retry = RedisAttemptLimiter.check(redis, properties.redisNamespace(), "user", userId.toString(),
                    remoteAddress, properties.userLimit(), properties.ipLimit(), properties.window());
            if (retry == null) throw unavailable();
            if (retry > 0) throw new DirectMessageRateLimitException(retry);
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    private BusinessException unavailable() {
        return new BusinessException("DIRECT_MESSAGE_UNAVAILABLE", "私信服务暂时不可用，请稍后重试",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
