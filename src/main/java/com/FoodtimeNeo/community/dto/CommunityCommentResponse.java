package com.FoodtimeNeo.community.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Community comment; admin queue and "mine" expose pending/rejected with AI verdicts. */
public record CommunityCommentResponse(UUID id, UUID postId, UUID userId, String userDisplayName, String content,
                                       int likeCount, String status, String reviewReason,
                                       String aiRiskLevel, BigDecimal aiConfidence, List<String> aiLabels,
                                       String aiReason, Instant aiCheckedAt, Instant createdAt) { }
