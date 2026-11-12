package com.FoodtimeNeo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Public registration capabilities. Only stable, non-sensitive facts are exposed; rate limits,
 * allowed-domain lists, account existence and deployment credentials stay server-side.
 */
@Schema(description = "当前部署的注册能力")
public record RegistrationPolicyResponse(
        @Schema(description = "open | invitation | closed", example = "open")
        String mode,

        @Schema(description = "当前是否允许发送注册邮箱验证码", example = "true")
        boolean canSendEmailCode,

        @Schema(description = "注册是否必须填写邀请码", example = "false")
        boolean invitationRequired,

        @Schema(description = "策略版本；变化时客户端应刷新策略与页面", example = "open:email")
        String revision) {
}
