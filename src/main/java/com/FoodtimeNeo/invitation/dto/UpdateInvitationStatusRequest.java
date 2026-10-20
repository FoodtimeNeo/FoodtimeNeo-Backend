package com.FoodtimeNeo.invitation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "启用或停用邀请码；停用后新注册不再接受，已注册账号不受影响")
public record UpdateInvitationStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Schema(description = "active启用，disabled停用", allowableValues = {"active", "disabled"})
        String status) { }
