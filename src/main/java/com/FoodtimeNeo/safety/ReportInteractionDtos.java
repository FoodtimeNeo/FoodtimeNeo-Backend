package com.FoodtimeNeo.safety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.CodePointLength;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReportInteractionDtos {
    private ReportInteractionDtos() {}

    public record FollowUpRequest(
            @NotBlank @Size(max = 500) String content,
            @Size(max = 3) List<String> evidenceUrls,
            @NotNull UUID clientRequestId) {
        public FollowUpRequest {
            evidenceUrls = evidenceUrls == null ? List.of() : List.copyOf(evidenceUrls);
        }
    }

    public record StaffFollowUpRequest(
            @NotBlank @Size(max = 500) String content,
            @Size(max = 2000) String internalNote,
            @Size(max = 3) List<String> evidenceUrls,
            @NotNull UUID clientRequestId) {
        public StaffFollowUpRequest {
            evidenceUrls = evidenceUrls == null ? List.of() : List.copyOf(evidenceUrls);
        }
    }

    public record FollowUp(UUID id, UUID reportId, String authorRole, String authorName,
                           String content, String internalNote, List<String> evidenceUrls,
                           Instant createdAt) {
        public FollowUp {
            evidenceUrls = List.copyOf(evidenceUrls);
        }
    }

    public record AppealRequest(
            @NotBlank @CodePointLength(max = 2000) String content,
            @Size(max = 3) List<String> evidenceUrls,
            @NotNull UUID clientRequestId) {
        public AppealRequest {
            evidenceUrls = evidenceUrls == null ? List.of() : List.copyOf(evidenceUrls);
        }
    }

    public record AppealResolveRequest(
            @NotBlank @Pattern(regexp = "upheld|rejected") String outcome,
            @NotBlank @Size(max = 500) String publicReason,
            @Size(max = 2000) String internalNote,
            @NotNull Long expectedRevision) {}

    public record Appeal(UUID id, UUID decisionId, UUID appellantId, String appellantName,
                         String side, String content, String status, String outcome,
                         String publicReason, String internalNote, long revision,
                         Instant createdAt, Instant resolvedAt, List<String> evidenceUrls) {
        public Appeal {
            evidenceUrls = List.copyOf(evidenceUrls);
        }
    }
}
