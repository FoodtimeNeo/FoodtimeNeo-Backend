package com.FoodtimeNeo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "一次性登录验证码；图片为 PNG base64，响应不得缓存")
public record CaptchaResponse(
        @Schema(description = "挑战标识，登录时原样带回") String challengeId,
        @Schema(description = "PNG 图片的 Base64，不包含答案或表达式") String imageBase64,
        @Schema(description = "挑战到期时刻，服务端时间为准") Instant expiresAt) {
}
