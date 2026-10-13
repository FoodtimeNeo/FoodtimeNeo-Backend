package com.FoodtimeNeo.review.entity;

import java.time.Instant;
import java.util.UUID;

public record ReviewSubmissionPageRow(UUID id, UUID dishId, String dishName, UUID stallId, String stallName,
                                      UUID diningHallId, String diningHallName, Integer rating, String content,
                                      String status, Instant submittedAt, Instant reviewedAt, String reviewReason,
                                      Instant updatedAt, Long total) { }
