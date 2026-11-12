package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.safety.ReportAppealService;
import com.FoodtimeNeo.safety.ReportFollowUpService;
import com.FoodtimeNeo.safety.ReportInteractionDtos;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@SecurityRequirement(name = "sessionCookie")
public class ReportInteractionController {
    private final ReportFollowUpService followUps;
    private final ReportAppealService appeals;

    public ReportInteractionController(ReportFollowUpService followUps, ReportAppealService appeals) {
        this.followUps = followUps;
        this.appeals = appeals;
    }

    @GetMapping("/reports/{id}/follow-ups")
    @Operation(summary = "我的举报追问记录", description = "仅举报人本人；不返回内部备注")
    public ApiResponse<PageResponse<ReportInteractionDtos.FollowUp>> followUps(
            @PathVariable UUID id, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        return ApiResponse.success(followUps.thread(actor(request), id, new PageRequest(page, size)));
    }

    @PostMapping("/reports/{id}/follow-ups")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "追加举报追问", description = "仅举报人；UTC+8 每日最多5次，每次500字、最多3张证据")
    public ApiResponse<ReportInteractionDtos.FollowUp> addFollowUp(@PathVariable UUID id,
            @Valid @RequestBody ReportInteractionDtos.FollowUpRequest body, HttpServletRequest request) {
        return ApiResponse.success(followUps.addReporter(actor(request), id, body));
    }

    @PostMapping("/moderation-decisions/{decisionId}/appeals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "针对决定提交申诉", description = "举报人与被举报人各自在入箱后7天内最多一次；2000字、最多3张证据")
    public ApiResponse<ReportInteractionDtos.Appeal> appeal(@PathVariable UUID decisionId,
            @Valid @RequestBody ReportInteractionDtos.AppealRequest body, HttpServletRequest request) {
        return ApiResponse.success(appeals.submit(actor(request), decisionId, body));
    }

    @GetMapping("/appeals")
    @Operation(summary = "我的申诉记录", description = "仅本人申诉；不返回内部备注")
    public ApiResponse<PageResponse<ReportInteractionDtos.Appeal>> myAppeals(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(appeals.mine(actor(request), new PageRequest(page, size)));
    }

    @GetMapping("/appeals/{id}")
    @Operation(summary = "我的申诉详情", description = "仅申诉人本人可读")
    public ApiResponse<ReportInteractionDtos.Appeal> myAppeal(@PathVariable UUID id,
            HttpServletRequest request) {
        return ApiResponse.success(appeals.mine(actor(request), id));
    }

    private static UserProfile actor(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
