package com.FoodtimeNeo.moderation.dto;
import java.util.UUID;
public record ModerationResult(UUID id, String status, UUID approvedId) { }
