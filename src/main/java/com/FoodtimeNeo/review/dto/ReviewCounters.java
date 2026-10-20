package com.FoodtimeNeo.review.dto;

public record ReviewCounters(long likeCount, long replyCount, boolean likedByMe, long counterVersion) { }
