package com.FoodtimeNeo.dish.dto;

import com.FoodtimeNeo.dish.entity.DishSubmissionPageRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "当前用户提交的菜品内容及审核结果，保留已停用档口的历史")
public record DishSubmissionResponse(
        @Schema(format = "uuid") UUID id, @Schema(format = "uuid") UUID stallId, String stallName,
        @Schema(format = "uuid") UUID diningHallId, String diningHallName, String name,
        @Schema(nullable = true) String description,
        @Schema(description = "提交参考价格，待审核或拒绝时可为null", implementation = Number.class,
                type = "number", minimum = "0", nullable = true) BigDecimal price,
        String imageUrl, @Schema(allowableValues = {"pending", "approved", "rejected"}) String status,
        @Schema(nullable = true) Instant reviewedAt, @Schema(nullable = true) String reviewReason,
        @Schema(description = "审核通过后创建的正式菜品ID，其余状态为null", format = "uuid", nullable = true) UUID approvedDishId,
        Instant createdAt, Instant updatedAt) {
    public static DishSubmissionResponse from(DishSubmissionPageRow row) {
        return new DishSubmissionResponse(row.id(), row.stallId(), row.stallName(), row.diningHallId(), row.diningHallName(),
                row.name(), row.description(), row.price(), row.imageUrl(), row.status(), row.reviewedAt(), row.reviewReason(),
                row.approvedDishId(), row.createdAt(), row.updatedAt());
    }
}
