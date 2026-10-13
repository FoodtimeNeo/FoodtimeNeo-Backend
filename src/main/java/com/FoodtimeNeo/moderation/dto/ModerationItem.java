package com.FoodtimeNeo.moderation.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** type and targetId describe which existing submission table/relation this item belongs to. */
public record ModerationItem(UUID id, String type, UUID targetId, UUID userId, String name,
        String content, BigDecimal price, String imageUrl, Integer rating, String status,
        String reason, Instant createdAt) { }
