package com.FoodtimeNeo.api.submission;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.submission.dto.*;
import com.FoodtimeNeo.submission.service.CatalogSubmissionService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "sessionCookie")
public class CatalogSubmissionController {
    private final CatalogSubmissionService submissions;
    public CatalogSubmissionController(CatalogSubmissionService submissions) { this.submissions = submissions; }

    @PostMapping("/stalls/{stallId}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "投稿菜品", description = "使用本人上传的图片，参考价格可空；pending不创建正式菜品")
    public ApiResponse<SubmissionCreatedResponse> dish(@PathVariable UUID stallId,
            @Valid @RequestBody SubmitDishRequest body, HttpServletRequest request) {
        return ApiResponse.success(submissions.dish(stallId, current(request).id(), body));
    }

    @PostMapping("/dining-halls/{diningHallId}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "投稿档口", description = "使用本人上传的封面，pending不创建正式档口，不包含楼层字段")
    public ApiResponse<SubmissionCreatedResponse> stall(@PathVariable UUID diningHallId,
            @Valid @RequestBody SubmitStallRequest body, HttpServletRequest request) {
        return ApiResponse.success(submissions.stall(diningHallId, current(request).id(), body));
    }

    @GetMapping("/users/me/stall-submissions")
    @Operation(summary = "我的档口投稿", description = "本人全部状态，保留停用食堂的历史与驳回原因")
    public ApiResponse<PageResponse<StallSubmissionResponse>> mine(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(submissions.stalls(current(request).id(), new PageRequest(page, size)));
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
