package com.FoodtimeNeo.auth.service;

import com.FoodtimeNeo.common.exception.RateLimitedException;

public class CaptchaRateLimitException extends RateLimitedException {
    public CaptchaRateLimitException(long retryAfter) {
        super("CAPTCHA_RATE_LIMITED", "验证码请求过于频繁，请稍后重试", retryAfter);
    }
}
