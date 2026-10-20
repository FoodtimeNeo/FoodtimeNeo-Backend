package com.FoodtimeNeo.review.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewReplyResponse(UUID id,UUID reviewId,UUID replyToId,UUID userId,String userDisplayName,
        String content,String status,String reviewReason,Instant reviewedAt,Instant createdAt,Instant updatedAt,
        UUID replyToUserId,String replyToUserDisplayName,boolean replyToDeleted,boolean ownedByMe,
        UUID dishId,UUID stallId,UUID diningHallId,String dishName,boolean reviewDeleted,
        long likeCount,boolean likedByMe,long counterVersion) { }
