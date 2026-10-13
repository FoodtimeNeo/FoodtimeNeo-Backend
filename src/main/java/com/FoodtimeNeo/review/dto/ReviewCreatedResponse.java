package com.FoodtimeNeo.review.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewCreatedResponse(UUID id, UUID dishId, int rating, String content,
                                    String status, Instant submittedAt) { }
