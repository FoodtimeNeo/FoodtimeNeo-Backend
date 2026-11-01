package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.follow.FollowDtos.FollowStateResponse;
import com.FoodtimeNeo.follow.FollowDtos.FollowUserResponse;
import com.FoodtimeNeo.follow.service.FollowService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Follow")
@SecurityRequirement(name = "sessionCookie")
public class UserFollowController {
    private final FollowService follows;

    public UserFollowController(FollowService follows) {
        this.follows = follows;
    }

    @PutMapping("/{userId}/follow")
    @Operation(summary = "关注用户", description = "幂等；不能关注自己或已停用账号")
    public ApiResponse<FollowStateResponse> follow(@PathVariable UUID userId, HttpServletRequest request) {
        return ApiResponse.success(follows.follow(current(request), userId, request.getRemoteAddr()));
    }

    @DeleteMapping("/{userId}/follow")
    @Operation(summary = "取消关注", description = "幂等")
    public ApiResponse<FollowStateResponse> unfollow(@PathVariable UUID userId, HttpServletRequest request) {
        return ApiResponse.success(follows.unfollow(current(request), userId, request.getRemoteAddr()));
    }

    @GetMapping("/{userId}/follow-status")
    @Operation(summary = "关注状态", description = "返回当前登录用户是否关注该用户及其粉丝数")
    public ApiResponse<FollowStateResponse> status(@PathVariable UUID userId, HttpServletRequest request) {
        return ApiResponse.success(follows.status(current(request), userId));
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "粉丝列表", description = "按关注时间倒序；任何登录用户可查看")
    public ApiResponse<PageResponse<FollowUserResponse>> followers(@PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ApiResponse.success(follows.followers(userId, current(request), new PageRequest(page, size)));
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "关注列表", description = "按关注时间倒序；任何登录用户可查看")
    public ApiResponse<PageResponse<FollowUserResponse>> following(@PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ApiResponse.success(follows.following(userId, current(request), new PageRequest(page, size)));
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
