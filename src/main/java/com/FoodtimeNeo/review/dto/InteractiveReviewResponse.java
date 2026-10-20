package com.FoodtimeNeo.review.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Keeps the published review contract flat while adding batched, viewer-specific counters. */
public record InteractiveReviewResponse(@JsonUnwrapped PublishedReviewResponse review,
        long likeCount, long replyCount, boolean likedByMe, long counterVersion) {
    public InteractiveReviewResponse(PublishedReviewResponse review, ReviewCounters counters) {
        this(review, counters.likeCount(), counters.replyCount(), counters.likedByMe(), counters.counterVersion());
    }
}
