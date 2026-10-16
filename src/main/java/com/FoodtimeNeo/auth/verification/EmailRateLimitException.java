package com.FoodtimeNeo.auth.verification;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class EmailRateLimitException extends BusinessException {
    private final long retryAfter;
    public EmailRateLimitException(long retryAfter) {
        super("EMAIL_SEND_RATE_LIMITED", "验证码发送过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfter = retryAfter;
    }
    public long getRetryAfter() { return retryAfter; }
}
