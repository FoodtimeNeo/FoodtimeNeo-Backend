package com.FoodtimeNeo.review.dto;

import java.time.Instant;
import java.util.UUID;

/** All context fields are obtained from the dish, stall and dining hall relations; the author name comes from the user record.
 * AI verdict fields are only populated on the admin path (joined from the source submission); public lists leave them null. */
public record PublishedReviewResponse(UUID id, UUID dishId, UUID userId, String userDisplayName, int rating, String content,
                                      Instant createdAt, Instant updatedAt, String dishName,
                                      UUID stallId, String stallName, UUID diningHallId, String diningHallName,
                                      String aiRiskLevel, java.math.BigDecimal aiConfidence, java.util.List<String> aiLabels,
                                      String aiReason, Instant aiCheckedAt) { }
