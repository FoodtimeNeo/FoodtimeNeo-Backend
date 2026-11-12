package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.safety.ReportActionDtos;
import com.FoodtimeNeo.safety.ReportActionService;
import com.FoodtimeNeo.safety.ReportDecisionDtos;
import com.FoodtimeNeo.safety.ReportDecisionService;
import com.FoodtimeNeo.safety.ReportFollowUpService;
import com.FoodtimeNeo.safety.ReportInteractionDtos;
import com.FoodtimeNeo.safety.SafetyService;
import jakarta.validation.Valid;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reports")
@SecurityRequirement(name = "sessionCookie")
public class ReportsAdminController {
    private final SafetyService safety;
    private final ReportDecisionService decisions;
    private final ReportFollowUpService followUps;
    private final ReportActionService actions;

    public ReportsAdminController(SafetyService safety, ReportDecisionService decisions,
            ReportFollowUpService followUps, ReportActionService actions) {
        this.safety = safety;
        this.decisions = decisions;
        this.followUps = followUps;
        this.actions = actions;
    }

    @GetMapping
    public ApiResponse<PageResponse<SafetyService.ReportRow>> pending(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(safety.pending(new PageRequest(page, size)));
    }

    @PostMapping("/{id}/decision")
    public ApiResponse<ReportDecisionDtos.DecisionResponse> decide(@PathVariable UUID id,
            @Valid @RequestBody ReportDecisionDtos.DecisionRequest body, HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(decisions.decide(user, id, body));
    }

    @GetMapping("/decisions")
    public ApiResponse<PageResponse<ReportDecisionDtos.AdminDecision>> decisions(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(decisions.adminDecisions(user, new PageRequest(page, size)));
    }

    @GetMapping("/{id}/follow-ups")
    public ApiResponse<PageResponse<ReportInteractionDtos.FollowUp>> followUps(@PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(followUps.adminThread(user, id, new PageRequest(page, size)));
    }

    @PostMapping("/{id}/follow-ups")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportInteractionDtos.FollowUp> reply(@PathVariable UUID id,
            @Valid @RequestBody ReportInteractionDtos.StaffFollowUpRequest body, HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(followUps.addStaff(user, id, body));
    }

    @GetMapping("/{id}/actions")
    public ApiResponse<java.util.List<ReportActionDtos.Response>> actions(@PathVariable UUID id,
            HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(actions.listForAdmin(user, id));
    }

    @PostMapping("/{id}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportActionDtos.Response> performAction(@PathVariable UUID id,
            @Valid @RequestBody ReportActionDtos.Request body, HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(actions.perform(user, id, body));
    }

    @PutMapping("/{id}/review")
    public ApiResponse<Void> review(@PathVariable UUID id, HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        safety.review(id, user.id());
        return ApiResponse.success(null);
    }
}
