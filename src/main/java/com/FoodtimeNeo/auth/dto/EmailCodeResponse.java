package com.FoodtimeNeo.auth.dto;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "验证码已提交SMTP服务；响应不包含验证码，不保证收件箱即时送达")
public record EmailCodeResponse(Instant expiresAt, long resendAfterSeconds) { }
