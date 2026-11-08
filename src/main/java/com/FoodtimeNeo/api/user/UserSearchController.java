package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.user.dto.UserSearchResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.service.UserSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/search")
@SecurityRequirement(name = "sessionCookie")
public class UserSearchController {
    private final UserSearchService searches;

    public UserSearchController(UserSearchService searches) {
        this.searches = searches;
    }

    @GetMapping
    @Operation(summary = "按用户名搜索用户", description = "按昵称不区分大小写包含匹配；完整匹配和前缀优先；仅活跃的其他用户；空关键词不列举用户")
    public ApiResponse<PageResponse<UserSearchResponse>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        UserProfile viewer = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(searches.search(viewer, q, new PageRequest(page, size)));
    }
}
