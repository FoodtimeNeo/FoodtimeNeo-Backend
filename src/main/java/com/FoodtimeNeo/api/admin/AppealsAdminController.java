package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.safety.ReportAppealService;
import com.FoodtimeNeo.safety.ReportInteractionDtos;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/appeals")
@SecurityRequirement(name = "sessionCookie")
public class AppealsAdminController {
    private final ReportAppealService appeals;

    public AppealsAdminController(ReportAppealService appeals) {
        this.appeals = appeals;
    }

    @GetMapping
    @Operation(summary = "申诉审核队列", description = "需要 appeal.review；默认仅超管")
    public ApiResponse<PageResponse<ReportInteractionDtos.Appeal>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ApiResponse.success(appeals.adminQueue(actor(request), status, new PageRequest(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "申诉审核详情", description = "需要 appeal.review；包含内部备注与证据")
    public ApiResponse<ReportInteractionDtos.Appeal> detail(@PathVariable UUID id, HttpServletRequest request) {
        return ApiResponse.success(appeals.adminDetail(actor(request), id));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "开始复核申诉", description = "submitted -> under_review；需要 appeal.review")
    public ApiResponse<ReportInteractionDtos.Appeal> start(@PathVariable UUID id,
            @RequestParam long expectedRevision, HttpServletRequest request) {
        return ApiResponse.success(appeals.start(actor(request), id, expectedRevision));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "完成申诉复核", description = "保存历史结论并通知双方；需要 appeal.review")
    public ApiResponse<ReportInteractionDtos.Appeal> resolve(@PathVariable UUID id,
            @Valid @RequestBody ReportInteractionDtos.AppealResolveRequest body, HttpServletRequest request) {
        return ApiResponse.success(appeals.resolve(actor(request), id, body));
    }

    private static UserProfile actor(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
