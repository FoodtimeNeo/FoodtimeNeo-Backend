package com.FoodtimeNeo.api.review;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.review.dto.*;
import com.FoodtimeNeo.review.service.ReviewReplyService;
import com.FoodtimeNeo.submission.service.SubmissionRateLimiter;
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
@SecurityRequirement(name="sessionCookie")
public class ReviewReplyController {
    private final ReviewReplyService replies;
    private final SubmissionRateLimiter limiter;
    public ReviewReplyController(ReviewReplyService replies,SubmissionRateLimiter limiter) {this.replies=replies;this.limiter=limiter;}
    @GetMapping("/reviews/{reviewId}/replies") @Operation(summary="菜品评论的已通过回复",description="游标分页，计数来自统计表；目标评论及目录须可见")
    public ApiResponse<CursorPageResponse<ReviewReplyResponse>> list(@PathVariable UUID reviewId,
            @RequestParam(defaultValue="20") int size,@RequestParam(required=false) String cursor,HttpServletRequest request) {
        return ApiResponse.success(replies.list(reviewId,current(request).id(),size,cursor));
    }
    @PostMapping("/reviews/{reviewId}/replies") @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary="提交菜品评论回复",description="使用clientRequestId去重，replyToId可空，进入AI/人工审核")
    public ApiResponse<ReviewReplyCreatedResponse> submit(@PathVariable UUID reviewId,@Valid @RequestBody CreateReviewReplyRequest body,HttpServletRequest request) {
        UserProfile user=current(request);limiter.check(user.id(),request.getRemoteAddr());
        return ApiResponse.success(replies.submit(reviewId,user.id(),body));
    }
    @GetMapping("/users/me/review-replies") @Operation(summary="我的回复及审核状态")
    public ApiResponse<PageResponse<ReviewReplyResponse>> mine(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        return ApiResponse.success(replies.mine(current(request).id(),new PageRequest(page,size)));
    }
    @DeleteMapping("/review-replies/{replyId}") @Operation(summary="删除自己的回复",description="软删除，审核历史和后续回复保留")
    public ApiResponse<Void> delete(@PathVariable UUID replyId,HttpServletRequest request) {
        replies.delete(replyId,current(request).id(),false);return ApiResponse.success(null);
    }
    private UserProfile current(HttpServletRequest request) {return (UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER);}
}
