package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.review.dto.PublishedReviewResponse;
import com.FoodtimeNeo.review.service.PublishedReviewService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/reviews")
@SecurityRequirement(name = "sessionCookie")
public class MyPublishedReviewController {
    private final PublishedReviewService reviews;
    public MyPublishedReviewController(PublishedReviewService reviews) { this.reviews = reviews; }

    @GetMapping
    @Operation(summary = "我的已发布评论", description = "仅本人正式评论，附关联名称；保留停用菜品的历史")
    public ApiResponse<PageResponse<PublishedReviewResponse>> list(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reviews.mine(current(request).id(), new PageRequest(page, size)));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "删除自己的评论", description = "只能删除本人正式评论；审核历史保留，评分同步更新")
    public ApiResponse<Void> delete(@PathVariable UUID reviewId, HttpServletRequest request) {
        reviews.delete(reviewId, current(request).id());
        return ApiResponse.success(null);
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
