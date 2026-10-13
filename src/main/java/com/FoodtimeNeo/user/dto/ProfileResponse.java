package com.FoodtimeNeo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "当前用户的个人资料；头像来自users.avatar_url，不返回敏感认证字段")
public record ProfileResponse(UUID id, String email, String displayName, String role,
                              @Schema(nullable = true) String avatarUrl) { }
