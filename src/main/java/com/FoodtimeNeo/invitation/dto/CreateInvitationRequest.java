package com.FoodtimeNeo.invitation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "创建邀请码；三项全可空，空标签、不限次数、长期有效")
public record CreateInvitationRequest(
        @Size(max = 100, message = "备注不能超过100个字符")
        @Schema(description = "管理备注，如发放对象或用途", nullable = true)
        String label,
        @Min(value = 1, message = "可用次数至少为1")
        @Schema(description = "可复用次数上限，空表示不限次数", nullable = true)
        Integer maxUses,
        @Future(message = "有效期须晚于当前时间")
        @Schema(description = "到期时间，空表示长期有效", nullable = true)
        Instant expiresAt) { }
