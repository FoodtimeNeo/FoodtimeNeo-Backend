package com.FoodtimeNeo.review.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewReplyCreatedResponse(UUID id,String status,Instant createdAt) { }
