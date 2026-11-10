package com.FoodtimeNeo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "用户搜索结果：公开昵称、头像及当前用户的关注状态")
public record UserSearchResponse(UUID id, String displayName, String avatarUrl, boolean followedByMe) { }
