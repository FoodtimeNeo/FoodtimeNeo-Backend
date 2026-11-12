package com.FoodtimeNeo.correction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CatalogCorrectionDtos {
    private CatalogCorrectionDtos() {}

    public record CreateRequest(
            @NotBlank String targetType,
            @NotNull UUID targetId,
            @NotNull Long baseRevision,
            @NotEmpty @Size(max = 3) List<String> changedFields,
            String name,
            String description,
            BigDecimal price,
            @NotBlank @Size(max = 500) String reason,
            @Size(max = 3) List<String> evidenceUrls,
            @NotBlank @Size(max = 64) String idempotencyKey) {
        public CreateRequest {
            changedFields = changedFields == null ? null : new java.util.ArrayList<>(changedFields);
            evidenceUrls = evidenceUrls == null ? List.of() : new java.util.ArrayList<>(evidenceUrls);
        }
    }

    public record DecisionRequest(
            @NotBlank String decision,
            @Size(max = 500) String reviewNote,
            @NotNull Long expectedRevision) {}

    public record Response(
            UUID id,
            String targetType,
            UUID targetId,
            String targetSummary,
            long baseRevision,
            String originalName,
            String originalDescription,
            BigDecimal originalPrice,
            String proposedName,
            String proposedDescription,
            BigDecimal proposedPrice,
            List<String> changedFields,
            String reason,
            String status,
            String reviewNote,
            Instant createdAt,
            Instant reviewedAt,
            long revision,
            List<String> evidenceUrls) {
        public Response {
            changedFields = List.copyOf(changedFields);
            evidenceUrls = List.copyOf(evidenceUrls);
        }
    }

    public record AdminResponse(
            Response correction,
            UUID submitterId,
            String submitterName,
            String submitterEmail) {}

    public record TargetSnapshot(
            String type,
            UUID id,
            long revision,
            String name,
            String description,
            BigDecimal price,
            String summary,
            boolean visible) {}
}
