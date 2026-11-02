package com.FoodtimeNeo.venue;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class VenueModels {
  private VenueModels() {}

  public record ReviewInput(
      @NotNull BigDecimal rating, @NotBlank String content, @NotNull UUID clientRequestId) {}

  public record ReplyInput(
      @NotBlank String content, UUID replyToId, @NotNull UUID clientRequestId) {}

  public record Reason(@NotBlank String reason) {}

  public record Bucket(int rating, long reviewCount) {}

  public record Target(
      UUID id,
      String type,
      UUID targetId,
      String name,
      UUID diningHallId,
      String diningHallName,
      UUID stallId,
      String stallName,
      BigDecimal averageScore,
      long reviewCount,
      List<Bucket> distribution) {}

  public record Row(
      UUID id,
      UUID reviewId,
      UUID submissionId,
      UUID userId,
      String userDisplayName,
      String targetType,
      UUID targetId,
      String name,
      Integer rating,
      String content,
      String status,
      String reviewReason,
      Instant createdAt,
      Instant updatedAt,
      boolean ownedByMe,
      long likeCount,
      long replyCount,
      boolean likedByMe,
      long counterVersion,
      UUID replyToId,
      String replyToUserDisplayName,
      boolean replyToDeleted,
      long publicationVersion) {}

  public record Mine(UUID reviewId, Row publicVersion, Row privateSubmission) {}

  public record Pending(String type, UUID id, int rating, String content) {}

  public record AuditDetail(boolean venue, Row publicVersion, Row submission) {}
}
