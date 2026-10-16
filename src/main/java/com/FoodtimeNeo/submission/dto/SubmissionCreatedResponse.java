package com.FoodtimeNeo.submission.dto;
import java.time.Instant;
import java.util.UUID;
public record SubmissionCreatedResponse(UUID id, String status, Instant createdAt) { }
