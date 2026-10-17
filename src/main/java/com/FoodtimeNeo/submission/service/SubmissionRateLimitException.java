package com.FoodtimeNeo.submission.service;

import com.FoodtimeNeo.common.exception.RateLimitedException;

public class SubmissionRateLimitException extends RateLimitedException {
    public SubmissionRateLimitException(long retryAfter) {
        super("SUBMISSION_RATE_LIMITED", "投稿或点评过于频繁，请稍后重试", retryAfter);
    }
}
