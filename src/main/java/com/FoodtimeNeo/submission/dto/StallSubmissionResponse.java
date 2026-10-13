package com.FoodtimeNeo.submission.dto;
import java.time.Instant;
import java.util.UUID;
public record StallSubmissionResponse(UUID id, UUID diningHallId, String diningHallName, String name,
        String description, String coverImageUrl, String status, Instant reviewedAt, String reviewReason,
        UUID approvedStallId, Instant createdAt, Instant updatedAt) { }
