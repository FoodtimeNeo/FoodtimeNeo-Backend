package com.FoodtimeNeo.api.review;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.review.dto.ReviewLikeResponse;
import com.FoodtimeNeo.review.service.ReviewActionRateLimiter;
import com.FoodtimeNeo.review.service.ReviewInteractionService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews/{reviewId}")
@SecurityRequirement(name="sessionCookie")
public class ReviewInteractionController {
    private final ReviewInteractionService interactions;
    private final ReviewActionRateLimiter limiter;
    public ReviewInteractionController(ReviewInteractionService interactions,ReviewActionRateLimiter limiter) {this.interactions=interactions;this.limiter=limiter;}
    @PutMapping("/like") @Operation(summary="点赞菜品评论",description="幂等；已删除或停用菜品下的评论不可点赞")
    public ApiResponse<ReviewLikeResponse> like(@PathVariable UUID reviewId,HttpServletRequest request) {return change(reviewId,request,true);}
    @DeleteMapping("/like") @Operation(summary="取消菜品评论点赞",description="幂等；允许取消不可见历史记录的本人点赞")
    public ApiResponse<ReviewLikeResponse> unlike(@PathVariable UUID reviewId,HttpServletRequest request) {return change(reviewId,request,false);}
    private ApiResponse<ReviewLikeResponse> change(UUID id,HttpServletRequest request,boolean liked) {
        UserProfile user=(UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        limiter.check(user.id(),request.getRemoteAddr());
        return ApiResponse.success(interactions.setLike(id,user.id(),liked));
    }
}
