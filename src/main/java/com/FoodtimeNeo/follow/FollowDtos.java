package com.FoodtimeNeo.follow;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Response shapes for the directed follow relation. */
public final class FollowDtos {
    private FollowDtos() { }

    @Schema(description = "关注状态与目标用户的粉丝数")
    public record FollowStateResponse(boolean followed, long followerCount) { }

    @Schema(description = "关注或粉丝列表中的用户；followedByMe 表示当前登录用户是否关注了该用户")
    public record FollowUserResponse(UUID id, String displayName, String avatarUrl, boolean followedByMe) { }
}
