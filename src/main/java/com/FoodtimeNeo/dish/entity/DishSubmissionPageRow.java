package com.FoodtimeNeo.dish.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DishSubmissionPageRow(UUID id, UUID stallId, String stallName, UUID diningHallId, String diningHallName,
                                    String name, String description, BigDecimal price, String imageUrl, String status,
                                    Instant reviewedAt, String reviewReason, UUID approvedDishId,
                                    Instant createdAt, Instant updatedAt, Long total) { }
