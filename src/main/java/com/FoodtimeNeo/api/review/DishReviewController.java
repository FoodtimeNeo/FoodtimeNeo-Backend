package com.FoodtimeNeo.api.review;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.review.dto.*;
import com.FoodtimeNeo.review.service.PublishedReviewService;
import com.FoodtimeNeo.submission.service.SubmissionRateLimiter;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dishes/{dishId}")
@Tag(name = "Reviews")
@SecurityRequirement(name = "sessionCookie")
public class DishReviewController {
    private final PublishedReviewService reviews;
    private final SubmissionRateLimiter limiter;
    public DishReviewController(PublishedReviewService reviews, SubmissionRateLimiter limiter) {
        this.reviews = reviews; this.limiter = limiter;
    }

    @GetMapping("/reviews")
    @Operation(summary = "查看已审核评论", description = "只返回正式评论；菜品及其父级必须启用，不返回头像")
    public ApiResponse<PageResponse<PublishedReviewResponse>> list(@PathVariable UUID dishId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reviews.list(dishId, new PageRequest(page, size)));
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "提交评论审核", description = "评分1到5，正文必填；用户从会话取得，重复待审核返回409")
    public ApiResponse<ReviewCreatedResponse> submit(@PathVariable UUID dishId,
            @Valid @RequestBody SubmitReviewRequest body, HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        limiter.check(user.id(), request.getRemoteAddr());
        return ApiResponse.success(reviews.submit(dishId, user.id(), body));
    }

    @GetMapping("/rating")
    @Operation(summary = "查询菜品评分快照", description = "使用每24小时刷新的物化视图；无评分平均分为null")
    public ApiResponse<DishRatingResponse> rating(@PathVariable UUID dishId) {
        return ApiResponse.success(reviews.rating(dishId));
    }
}
