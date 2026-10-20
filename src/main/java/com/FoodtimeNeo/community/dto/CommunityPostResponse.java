package com.FoodtimeNeo.community.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Community post view: public feed only carries approved rows, the admin queue
 * and "mine" also expose pending/rejected ones with their AI verdict.
 * likedByMe/favoritedByMe are computed for the current user.
 */
public record CommunityPostResponse(UUID id, UUID userId, String userDisplayName, String content,
                                    List<String> imageUrls, int likeCount, int commentCount, int repostCount,
                                    UUID repostOfId, RepostPreview repost, boolean likedByMe, boolean favoritedByMe,
                                    String status, String reviewReason,
                                    String aiRiskLevel, BigDecimal aiConfidence, List<String> aiLabels,
                                    String aiReason, Instant aiCheckedAt, Instant createdAt) {
    public record RepostPreview(UUID id, String userDisplayName, String content) { }
}
