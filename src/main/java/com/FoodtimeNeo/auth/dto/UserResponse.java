package com.FoodtimeNeo.auth.dto;

import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "当前用户的公开信息，不包含密码及会话凭据")
public record UserResponse(UUID id, String email, String displayName, String role, String sessionMode) {
    public UserResponse(UUID id, String email, String displayName, String role) {
        this(id, email, displayName, role, "normal");
    }

    public static UserResponse from(UserProfile user) {
        return new UserResponse(user.id(), user.email(), user.displayName(), user.role(), "normal");
    }

    public static UserResponse from(UserProfile user, String sessionMode) {
        return new UserResponse(user.id(), user.email(), user.displayName(), user.role(), sessionMode);
    }
}
