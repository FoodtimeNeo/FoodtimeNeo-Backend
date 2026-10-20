package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.community.dto.CommunityPostResponse;
import com.FoodtimeNeo.community.service.CommunityInteractionService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/community-favorites")
@SecurityRequirement(name = "sessionCookie")
public class MyCommunityFavoriteController {
    private final CommunityInteractionService interactions;
    public MyCommunityFavoriteController(CommunityInteractionService interactions) { this.interactions = interactions; }

    @GetMapping
    @Operation(summary = "我的收藏", description = "已收藏且仍可见的动态（已通过或本人发布），最新收藏在前")
    public ApiResponse<PageResponse<CommunityPostResponse>> favorites(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(interactions.myFavorites(user.id(), new PageRequest(page, size)));
    }
}
