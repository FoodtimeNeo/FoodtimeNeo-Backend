package com.FoodtimeNeo.api.recommendation;

import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.recommendation.DailyRecommendationService;
import com.FoodtimeNeo.recommendation.dto.DailyRecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendations", description = "全站统一每日菜品推荐")
public class DailyRecommendationController {
    private final DailyRecommendationService recommendations;

    public DailyRecommendationController(DailyRecommendationService recommendations) { this.recommendations = recommendations; }

    @GetMapping(value = "/daily", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "获取每日推荐", description = "全站同日返回同一份推荐，不读取用户个性化行为")
    public ApiResponse<DailyRecommendationResponse> daily() {
        return ApiResponse.success(recommendations.today());
    }
}
