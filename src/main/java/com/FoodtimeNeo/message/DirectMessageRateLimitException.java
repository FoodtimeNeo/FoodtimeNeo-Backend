package com.FoodtimeNeo.message;

import com.FoodtimeNeo.common.exception.RateLimitedException;

public class DirectMessageRateLimitException extends RateLimitedException {
    public DirectMessageRateLimitException(long retryAfter) {
        super("DIRECT_MESSAGE_RATE_LIMITED", "私信发送过于频繁，请稍后重试", retryAfter);
    }
}
