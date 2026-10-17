package com.FoodtimeNeo.common.exception;

import org.springframework.http.HttpStatus;

/** Base for quota rejections; handlers add the Retry-After header. */
public abstract class RateLimitedException extends BusinessException {
    private final long retryAfter;
    protected RateLimitedException(String code, String message, long retryAfter) {
        super(code, message, HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfter = retryAfter;
    }
    public long getRetryAfter() { return retryAfter; }
}
