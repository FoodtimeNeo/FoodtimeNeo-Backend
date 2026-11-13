package com.FoodtimeNeo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "注册成功的用户信息，不包含密码或密码哈希")
public record RegisterResponse(
        @Schema(description = "用户唯一标识") UUID id,
        @Schema(description = "规范化邮箱") String email,
        @Schema(description = "默认昵称", example = "干饭人012345") String displayName,
        @Schema(description = "当前策略下的有效权限角色，user 或 visitor；不代表学校邮箱认证", example = "user") String role) { }
