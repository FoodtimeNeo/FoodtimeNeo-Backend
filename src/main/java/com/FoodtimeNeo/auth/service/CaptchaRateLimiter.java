package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.CaptchaProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Limits challenge issuance separately from login attempts; values are configurable pending Q18. */
@Service
public class CaptchaRateLimiter {
    private final StringRedisTemplate redis;
    private final CaptchaProperties properties;

    public CaptchaRateLimiter(StringRedisTemplate redis, CaptchaProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void check(String deviceId, String remoteAddress) {
        if (!properties.enabled()) return;
        try {
            Long retry = RedisAttemptLimiter.check(redis, properties.redisNamespace() + ":issue", "device",
                    deviceId == null || deviceId.isBlank() ? "unidentified" : deviceId,
                    remoteAddress, properties.issueDeviceLimit(), properties.issueIpLimit(),
                    properties.issueWindow());
            if (retry == null) throw unavailable();
            if (retry > 0) throw new CaptchaRateLimitException(retry);
        } catch (DataAccessException failure) {
            throw unavailable();
        }
    }

    private static BusinessException unavailable() {
        return new BusinessException("CAPTCHA_UNAVAILABLE", "验证码服务暂时不可用，请稍后重试",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
