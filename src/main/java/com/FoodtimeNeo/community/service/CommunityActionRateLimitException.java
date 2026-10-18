package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.common.exception.RateLimitedException;

public class CommunityActionRateLimitException extends RateLimitedException {
    public CommunityActionRateLimitException(long retryAfter) {
        super("COMMUNITY_ACTION_RATE_LIMITED", "社区互动过于频繁，请稍后重试", retryAfter);
    }
}
