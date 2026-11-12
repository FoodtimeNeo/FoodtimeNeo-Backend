package com.FoodtimeNeo.safety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ReportDecisionDtos {
    private ReportDecisionDtos() {}

    public record DecisionRequest(
            @NotBlank @Pattern(regexp = "upheld|not_upheld") String outcome,
            @NotBlank @Size(max = 500) String publicReason,
            @Size(max = 2000) String internalNote) {}

    public record DecisionResponse(UUID id, UUID reportId, String outcome, String publicReason,
                                   String internalNote, UUID reviewerId, Instant createdAt) {}

    public record MyReport(UUID id, String targetType, UUID targetId, UUID postId, String reason,
                           String excerpt, String status, Instant createdAt, UUID decisionId,
                           String outcome, String publicReason, Instant decidedAt) {}

    public record MyDecision(UUID decisionId, UUID reportId, String side, String targetType,
                             String outcome, String publicReason, Instant deliveredAt,
                             Instant appealDeadline) {}
}
