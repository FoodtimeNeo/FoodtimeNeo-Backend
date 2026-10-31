package com.FoodtimeNeo.dining.dto;

import com.FoodtimeNeo.dining.entity.StallImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "档口图集图片，按 sort_order、ID 升序排列")
public record StallImageResponse(
        @Schema(description = "图片ID", format = "uuid") UUID id,
        @Schema(description = "图片地址或存储引用") String imageUrl,
        @Schema(description = "排序值，越小越靠前") Integer sortOrder) {
    public static StallImageResponse from(StallImage image) {
        return new StallImageResponse(image.id(), image.imageUrl(), image.sortOrder());
    }
}
