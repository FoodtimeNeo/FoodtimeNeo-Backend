package com.FoodtimeNeo.moderation.dto;
import java.time.Instant;
import java.util.UUID;
public record ModerationLogResponse(UUID id, UUID submissionId, String type, UUID moderatorId, String actor,
        String aiModel, String action, String reason, Instant createdAt,
        UUID userId, String userEmail, String userDisplayName, String name, String content,
        String imageUrl, Instant submittedAt) { }
