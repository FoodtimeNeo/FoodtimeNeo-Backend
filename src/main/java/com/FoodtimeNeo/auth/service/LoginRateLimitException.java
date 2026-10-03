package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class LoginRateLimitException extends BusinessException {
    private final long retryAfter;

    public LoginRateLimitException(long retryAfter) {
        super("LOGIN_RATE_LIMITED", "登录尝试过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfter = retryAfter;
    }

    public long getRetryAfter() { return retryAfter; }
}
