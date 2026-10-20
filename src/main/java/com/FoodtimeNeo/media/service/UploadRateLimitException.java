package com.FoodtimeNeo.media.service;

import com.FoodtimeNeo.common.exception.RateLimitedException;

public class UploadRateLimitException extends RateLimitedException {
    public UploadRateLimitException(long retryAfter) {
        super("UPLOAD_RATE_LIMITED", "图片上传过于频繁，请稍后重试", retryAfter);
    }
}
