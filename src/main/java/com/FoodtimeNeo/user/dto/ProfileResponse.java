package com.FoodtimeNeo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "当前用户的个人资料；role 为当前策略下的有效权限角色，学校认证以 schoolEmailVerified 为准，不返回敏感认证字段")
public record ProfileResponse(UUID id, String email, String displayName, String role,
                              @Schema(nullable = true) String avatarUrl,
                              @Schema(description = "我关注的人数") long followingCount,
                              @Schema(description = "关注我的人数") long followerCount,
                              @Schema(description = "已绑定的学校邮箱；未绑定为 null", nullable = true) String schoolEmail,
                              @Schema(description = "学校邮箱是否已验证") boolean schoolEmailVerified) { }
