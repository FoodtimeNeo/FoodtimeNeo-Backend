package com.FoodtimeNeo.api.community;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.community.dto.CommunityPostResponse;
import com.FoodtimeNeo.community.dto.CreatePostRequest;
import com.FoodtimeNeo.community.service.CommunityPostService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community")
@SecurityRequirement(name = "sessionCookie")
public class CommunityController {
    private final CommunityPostService posts;
    public CommunityController(CommunityPostService posts) { this.posts = posts; }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "发布社区动态", description = "文字+本人已上传图片，进入AI即审；结论够硬自动过，否则留给人工")
    public ApiResponse<Map<String, UUID>> submit(@Valid @RequestBody CreatePostRequest body, HttpServletRequest request) {
        return ApiResponse.success(posts.submit(current(request).id(), body, request.getRemoteAddr()));
    }

    @GetMapping("/feed")
    @Operation(summary = "社区信息流", description = "只返回已通过动态，最新发布在前")
    public ApiResponse<PageResponse<CommunityPostResponse>> feed(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(posts.feed(current(request).id(), new PageRequest(page, size)));
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "动态详情", description = "已通过对所有登录用户可见；pending仅本人可见")
    public ApiResponse<CommunityPostResponse> detail(@PathVariable UUID id, HttpServletRequest request) {
        return ApiResponse.success(posts.detail(id, current(request).id()));
    }

    @DeleteMapping("/posts/{id}")
    @Operation(summary = "删除自己的动态", description = "只能删除本人的动态；审核记录保留")
    public ApiResponse<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        posts.delete(id, current(request).id());
        return ApiResponse.success(null);
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
