package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.community.service.CommunityModerationService;
import com.FoodtimeNeo.moderation.dto.ModerationItem;
import com.FoodtimeNeo.moderation.dto.ModerationRequest;
import com.FoodtimeNeo.moderation.dto.ModerationResult;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/community")
@SecurityRequirement(name = "sessionCookie")
public class CommunityAdminController {
    private final CommunityModerationService moderation;
    public CommunityAdminController(CommunityModerationService moderation) { this.moderation = moderation; }

    @GetMapping("/posts")
    @Operation(summary = "社区动态审核队列", description = "按状态分页，附AI分诊结论；AI自动过的只出现在approved里")
    public ApiResponse<PageResponse<ModerationItem>> queue(HttpServletRequest request,
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(moderation.queue(current(request), status, new PageRequest(page, size)));
    }

    @PostMapping("/posts/{id}")
    @Operation(summary = "通过或驳回社区动态", description = "仅处理pending；与AI自动决定共用同一事务路径")
    public ApiResponse<ModerationResult> moderate(@PathVariable UUID id, @Valid @RequestBody ModerationRequest body,
                                                 HttpServletRequest request) {
        return ApiResponse.success(moderation.moderate(current(request), "community-post", id, body));
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
