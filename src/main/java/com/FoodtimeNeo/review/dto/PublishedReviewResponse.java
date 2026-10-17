package com.FoodtimeNeo.review.dto;

import java.time.Instant;
import java.util.UUID;

/** All context fields are obtained from the dish, stall and dining hall relations; the author name comes from the user record. */
public record PublishedReviewResponse(UUID id, UUID dishId, UUID userId, String userDisplayName, int rating, String content,
                                      Instant createdAt, Instant updatedAt, String dishName,
                                      UUID stallId, String stallName, UUID diningHallId, String diningHallName) { }
