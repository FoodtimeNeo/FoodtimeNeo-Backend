package com.FoodtimeNeo.recommendation.dto;

import com.FoodtimeNeo.recommendation.DailyRecommendationStore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "每日推荐菜品")
public record RecommendedDishResponse(
        UUID dishId, String dishName, UUID diningHallId, String diningHallName,
        UUID stallId, String stallName, BigDecimal averageRating, long reviewCount,
        String imageUrl, String recommendationType, double recommendationScore,
        String reasonCode, String reason) {
    public static RecommendedDishResponse from(DailyRecommendationStore.RecommendationRow row) {
        // A rating may change after generation; do not retain a now unsupported reputation claim.
        String reasonCode = row.reasonCode();
        if ("GOOD_REPUTATION".equals(reasonCode) && (row.reviewCount() < 5 || row.averageRating() == null
                || row.averageRating().compareTo(BigDecimal.valueOf(3.75)) < 0)) {
            reasonCode = "DAILY_DISCOVERY";
        }
        String reason = switch (reasonCode) {
            case "GOOD_REPUTATION" -> "口碑不错";
            case "RECENTLY_POPULAR" -> "近期受到关注";
            case "LESS_EXPOSED" -> "发现更多菜品";
            default -> "今日尝鲜推荐";
        };
        return new RecommendedDishResponse(row.dishId(), row.dishName(), row.diningHallId(), row.diningHallName(),
                row.stallId(), row.stallName(), row.averageRating(), row.reviewCount(), row.imageUrl(),
                row.type(), row.score(), reasonCode, reason);
    }
}
