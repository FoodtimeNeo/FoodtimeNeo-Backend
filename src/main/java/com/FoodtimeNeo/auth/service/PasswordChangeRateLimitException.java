package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PasswordChangeRateLimitException extends BusinessException {
    private final long retryAfter;
    public PasswordChangeRateLimitException(long retryAfter) {
        super("PASSWORD_CHANGE_RATE_LIMITED", "修改密码尝试过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfter = retryAfter;
    }
    public long getRetryAfter() { return retryAfter; }
}
