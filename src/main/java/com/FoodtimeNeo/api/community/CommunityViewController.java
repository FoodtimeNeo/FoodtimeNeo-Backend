package com.FoodtimeNeo.api.community;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.community.dto.RecordPostViewRequest;
import com.FoodtimeNeo.community.service.CommunityViewService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@SecurityRequirement(name="sessionCookie")
public class CommunityViewController {
    private final CommunityViewService views;
    public CommunityViewController(CommunityViewService views) { this.views=views; }
    @PostMapping("/api/v1/community/posts/{id}/views")
    @Operation(summary="记录一次详情打开",description="每次进入使用新的请求编号；同次打开重试复用编号。列表、刷新和作者待审核预览不累计。")
    public ApiResponse<Map<String,Long>> record(@PathVariable UUID id,@Valid @RequestBody RecordPostViewRequest body,HttpServletRequest request) {
        var actor=(UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(Map.of("viewCount",views.record(id,actor.id(),body.clientRequestId())));
    }
}
