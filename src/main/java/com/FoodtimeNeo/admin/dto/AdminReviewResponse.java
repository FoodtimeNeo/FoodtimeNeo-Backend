package com.FoodtimeNeo.admin.dto;

import java.time.Instant;
import java.util.UUID;

/** Author email is only available through the guarded admin review endpoint. */
public record AdminReviewResponse(UUID id, UUID dishId, UUID userId, String userDisplayName, int rating, String content,
        Instant createdAt, Instant updatedAt, String dishName, UUID stallId, String stallName,
        UUID diningHallId, String diningHallName, String aiRiskLevel, java.math.BigDecimal aiConfidence,
        java.util.List<String> aiLabels, String aiReason, Instant aiCheckedAt, String userEmail) { }
