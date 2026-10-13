package com.FoodtimeNeo.dish.dto;

import com.FoodtimeNeo.dish.entity.DishDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "启用菜品的基础详情及所属档口、食堂")
public record DishDetailResponse(
        @Schema(format = "uuid") UUID id,
        @Schema(format = "uuid") UUID stallId,
        String stallName,
        @Schema(format = "uuid") UUID diningHallId,
        String diningHallName,
        String name,
        @Schema(description = "菜品简介，未设置时为null", nullable = true) String description,
        @Schema(description = "非负菜品价格，最多两位小数", implementation = Number.class, type = "number", minimum = "0") BigDecimal price) {
    public static DishDetailResponse from(DishDetail dish) {
        return new DishDetailResponse(dish.id(), dish.stallId(), dish.stallName(), dish.diningHallId(),
                dish.diningHallName(), dish.name(), dish.description(), dish.price());
    }
}
