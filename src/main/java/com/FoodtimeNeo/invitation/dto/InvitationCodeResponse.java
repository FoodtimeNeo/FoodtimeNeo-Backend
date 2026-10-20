package com.FoodtimeNeo.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "邀请码管理视图；明文码只在创建响应中返回一次，列表不再给出")
public record InvitationCodeResponse(UUID id, @Schema(nullable = true) String label,
                                     String status, @Schema(nullable = true) Integer maxUses,
                                     int usedCount, @Schema(nullable = true) Instant expiresAt,
                                     Instant createdAt) { }
