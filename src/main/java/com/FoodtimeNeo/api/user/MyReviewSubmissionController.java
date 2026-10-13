package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.review.dto.ReviewSubmissionResponse;
import com.FoodtimeNeo.review.service.ReviewSubmissionService;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/review-submissions")
@Tag(name = "User", description = "当前用户账号与个人记录")
public class MyReviewSubmissionController {
    private final ReviewSubmissionService submissions;
    public MyReviewSubmissionController(ReviewSubmissionService submissions) { this.submissions = submissions; }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "获取我的评论审核记录", description = "从校验后的会话取得用户ID，分页查询本人全部审核状态；保留已停用菜品历史，按提交时间倒序、ID升序排列")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "返回本人分页记录，无记录或超出末页时items为空"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "分页参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或会话失效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "查询或认证服务不可用")
    })
    public ApiResponse<PageResponse<ReviewSubmissionResponse>> list(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(description = "页码，从1开始", schema = @Schema(minimum = "1")) @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，1到100", schema = @Schema(minimum = "1", maximum = "100")) @RequestParam(defaultValue = "20") int size) {
        UserProfile current = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(submissions.list(current.id(), new PageRequest(page, size)));
    }
}
