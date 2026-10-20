package com.FoodtimeNeo.review.service;

import com.FoodtimeNeo.common.exception.RateLimitedException;

public class ReviewActionRateLimitException extends RateLimitedException {
    public ReviewActionRateLimitException(long retryAfter) {
        super("REVIEW_ACTION_RATE_LIMITED","评论互动过于频繁，请稍后重试",retryAfter);
    }
}
