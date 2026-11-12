package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.safety.ReportActionDtos;
import com.FoodtimeNeo.safety.ReportActionService;
import com.FoodtimeNeo.safety.ReportDecisionDtos;
import com.FoodtimeNeo.safety.ReportDecisionService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@SecurityRequirement(name = "sessionCookie")
public class ReportReceiptController {
    private final ReportDecisionService decisions;
    private final ReportActionService actions;

    public ReportReceiptController(ReportDecisionService decisions, ReportActionService actions) {
        this.decisions = decisions;
        this.actions = actions;
    }

    @GetMapping("/reports")
    @Operation(summary = "我的举报", description = "仅本人举报；含公开决定与历史记录状态")
    public ApiResponse<PageResponse<ReportDecisionDtos.MyReport>> myReports(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(decisions.myReports(actor(request), new PageRequest(page, size)));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "我的举报详情", description = "仅举报人本人可读")
    public ApiResponse<ReportDecisionDtos.MyReport> myReport(@PathVariable UUID id, HttpServletRequest request) {
        return ApiResponse.success(decisions.myReport(actor(request), id));
    }

    @GetMapping("/moderation-decisions")
    @Operation(summary = "我收到的处理决定", description = "举报人与被举报人分别投影；被举报人不获得举报人信息")
    public ApiResponse<PageResponse<ReportDecisionDtos.MyDecision>> myDecisions(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(decisions.myDecisions(actor(request), new PageRequest(page, size)));
    }

    @GetMapping("/moderation-decisions/{id}")
    @Operation(summary = "处理决定详情", description = "仅决定接收方本人可读，含入箱时间与 7 天申诉截止")
    public ApiResponse<ReportDecisionDtos.MyDecision> myDecision(@PathVariable UUID id,
            HttpServletRequest request) {
        return ApiResponse.success(decisions.myDecision(actor(request), id));
    }

    @GetMapping("/moderation-decisions/{id}/actions")
    @Operation(summary = "决定关联的真实管理动作", description = "仅决定接收方本人可读")
    public ApiResponse<java.util.List<ReportActionDtos.Response>> actions(@PathVariable UUID id,
            HttpServletRequest request) {
        return ApiResponse.success(actions.listForRecipient(actor(request), id));
    }

    private static UserProfile actor(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
