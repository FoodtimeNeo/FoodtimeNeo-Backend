package com.FoodtimeNeo.review.service;

import com.FoodtimeNeo.auth.security.RedisAttemptLimiter;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.common.exception.RateLimitedException;
import com.FoodtimeNeo.config.properties.ReviewActionLimitsProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ReviewActionRateLimiter {
    private final StringRedisTemplate redis;
    private final ReviewActionLimitsProperties limits;
    public ReviewActionRateLimiter(StringRedisTemplate redis,ReviewActionLimitsProperties limits) {this.redis=redis;this.limits=limits;}
    public void check(UUID user,String ip) {
        try {
            Long retry=RedisAttemptLimiter.check(redis,limits.redisNamespace(),"user",user.toString(),ip,
                    limits.userLimit(),limits.ipLimit(),limits.window());
            if (retry==null) throw unavailable();
            if (retry>0) throw new ReviewActionRateLimitException(retry);
        } catch(DataAccessException e) {throw unavailable();}
    }
    private BusinessException unavailable() {return new BusinessException("REVIEW_ACTION_UNAVAILABLE","评论互动暂时不可用",HttpStatus.SERVICE_UNAVAILABLE);}
}
