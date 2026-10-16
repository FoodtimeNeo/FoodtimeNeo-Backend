package com.FoodtimeNeo.dish.dto;

import com.FoodtimeNeo.dish.entity.Dish;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "启用菜品的列表展示信息")
public record DishListItemResponse(
        @Schema(description = "菜品ID", format = "uuid") UUID id,
        @Schema(description = "所属档口ID", format = "uuid") UUID stallId,
        @Schema(description = "菜品名称") String name,
        @Schema(description = "菜品价格，非负且最多两位小数", implementation = Number.class, type = "number", minimum = "0") BigDecimal price,
        @Schema(description = "每日评分快照；暂无评分时为null", nullable = true) BigDecimal averageScore,
        @Schema(description = "快照中的正式评论数，无快照时为0") long reviewCount) {
    public static DishListItemResponse from(Dish dish) {
        return new DishListItemResponse(dish.id(), dish.stallId(), dish.name(), dish.price(), dish.averageScore(), dish.reviewCount());
    }
}
