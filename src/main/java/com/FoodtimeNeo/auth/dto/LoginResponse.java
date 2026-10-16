package com.FoodtimeNeo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "登录用户与会话的固定到期时间；凭据仅通过HttpOnly Cookie传递")
public record LoginResponse(UserResponse user, Instant expiresAt) { }
