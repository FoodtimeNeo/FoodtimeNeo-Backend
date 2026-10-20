package com.FoodtimeNeo.review.dto;

public record ReviewLikeResponse(boolean likedByMe, long likeCount, long counterVersion) { }
