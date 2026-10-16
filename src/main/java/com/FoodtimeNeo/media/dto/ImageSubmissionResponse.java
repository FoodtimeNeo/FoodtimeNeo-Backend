package com.FoodtimeNeo.media.dto;
import java.time.Instant;
import java.util.UUID;
public record ImageSubmissionResponse(UUID id,UUID dishId,String dishName,UUID stallId,String stallName,
        UUID diningHallId,String diningHallName,String imageUrl,String status,Instant reviewedAt,
        String reviewReason,Instant createdAt) { }
