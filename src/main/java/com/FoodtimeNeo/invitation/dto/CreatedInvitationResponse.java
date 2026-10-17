package com.FoodtimeNeo.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "邀请码创建结果；明文码只返回一次，请妥善保存，服务端不再保存明文")
public record CreatedInvitationResponse(UUID id, String code, @Schema(nullable = true) String label,
                                        String status, @Schema(nullable = true) Integer maxUses,
                                        @Schema(nullable = true) Instant expiresAt, Instant createdAt) { }
