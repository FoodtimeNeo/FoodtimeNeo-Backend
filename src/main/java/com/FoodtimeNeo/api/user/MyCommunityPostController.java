package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.community.dto.CommunityPostResponse;
import com.FoodtimeNeo.community.service.CommunityInteractionService;
import com.FoodtimeNeo.community.service.CommunityPostService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/community-posts")
@SecurityRequirement(name = "sessionCookie")
public class MyCommunityPostController {
    private final CommunityPostService posts;
    public MyCommunityPostController(CommunityPostService posts) { this.posts = posts; }

    @GetMapping
    @Operation(summary = "我的社区动态", description = "含待审核、已通过和已驳回，附AI结论与驳回原因")
    public ApiResponse<PageResponse<CommunityPostResponse>> mine(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(posts.mine(user.id(), new PageRequest(page, size)));
    }
}
