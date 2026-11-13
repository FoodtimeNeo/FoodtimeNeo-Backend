package com.FoodtimeNeo.safety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ReportActionDtos {
    private ReportActionDtos() {}

    public record Request(
            @NotBlank @Pattern(regexp = "remove_post|remove_comment|disable_user|restore_user") String actionType,
            UUID targetId,
            @Size(max = 500) String note) {}

    public record Response(UUID id, UUID decisionId, String actionType, String targetType,
                           UUID targetId, UUID performedBy, String note, Instant createdAt) {}
}
