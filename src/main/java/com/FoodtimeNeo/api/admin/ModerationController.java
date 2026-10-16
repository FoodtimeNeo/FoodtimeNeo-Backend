package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.moderation.dto.*;
import com.FoodtimeNeo.moderation.service.ModerationService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@SecurityRequirement(name = "sessionCookie")
public class ModerationController {
    private final ModerationService moderation;
    public ModerationController(ModerationService moderation) { this.moderation = moderation; }
    @GetMapping
    @Operation(summary = "管理员审核队列", description = "评论、菜品、档口及图片提交，按状态分页；type为表来源的协议标识")
    public ApiResponse<PageResponse<ModerationItem>> queue(HttpServletRequest request,
            @RequestParam(defaultValue = "pending") String status, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(moderation.queue(current(request),status,new PageRequest(page,size)));
    }
    @PostMapping("/{type}/{submissionId}")
    @Operation(summary = "通过或驳回提交", description = "仅处理pending；正式数据、状态和日志在同一事务中完成，重复审核409")
    public ApiResponse<ModerationResult> moderate(@PathVariable String type, @PathVariable UUID submissionId,
            @Valid @RequestBody ModerationRequest body, HttpServletRequest request) {
        return ApiResponse.success(moderation.moderate(current(request),type,submissionId,body));
    }
    @GetMapping("/logs")
    @Operation(summary = "审核日志", description = "仅管理员可读取，按时间倒序分页")
    public ApiResponse<PageResponse<ModerationLogResponse>> logs(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(moderation.logs(current(request),new PageRequest(page,size)));
    }
    private UserProfile current(HttpServletRequest request) { return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER); }
}
