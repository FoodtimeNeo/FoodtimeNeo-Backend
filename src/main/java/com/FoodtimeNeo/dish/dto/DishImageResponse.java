package com.FoodtimeNeo.dish.dto;

import com.FoodtimeNeo.dish.entity.DishImagePageRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "已通过审核的菜品图片，不包含上传者身份及审核内部信息")
public record DishImageResponse(@Schema(format = "uuid") UUID id,
                                @Schema(format = "uuid") UUID dishId,
                                String imageUrl, Instant createdAt) {
    public static DishImageResponse from(DishImagePageRow row) {
        return new DishImageResponse(row.id(), row.dishId(), row.imageUrl(), row.createdAt());
    }
}
