package com.FoodtimeNeo.review.dto;

import com.FoodtimeNeo.review.entity.ReviewSubmissionPageRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "当前用户的评论审核提交，保留已停用菜品的历史")
public record ReviewSubmissionResponse(
        @Schema(format = "uuid") UUID id, @Schema(format = "uuid") UUID dishId, String dishName,
        @Schema(format = "uuid") UUID stallId, String stallName,
        @Schema(format = "uuid") UUID diningHallId, String diningHallName,
        @Schema(minimum = "1", maximum = "5") int rating, String content,
        @Schema(allowableValues = {"pending", "approved", "rejected"}) String status, Instant submittedAt,
        @Schema(description = "未审核时为null", nullable = true) Instant reviewedAt,
        @Schema(description = "审核说明，未填写时为null", nullable = true) String reviewReason, Instant updatedAt) {
    public static ReviewSubmissionResponse from(ReviewSubmissionPageRow row) {
        return new ReviewSubmissionResponse(row.id(), row.dishId(), row.dishName(), row.stallId(), row.stallName(),
                row.diningHallId(), row.diningHallName(), row.rating(), row.content(), row.status(), row.submittedAt(),
                row.reviewedAt(), row.reviewReason(), row.updatedAt());
    }
}
