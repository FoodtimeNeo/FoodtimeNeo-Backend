package com.FoodtimeNeo.dining.dto;

import com.FoodtimeNeo.dining.entity.DiningHall;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "启用食堂的展示信息；图片、简介和经纬度允许为空")
public record DiningHallResponse(
        @Schema(description = "食堂ID", format = "uuid") UUID id,
        @Schema(description = "食堂名称") String name,
        @Schema(description = "封面图片地址或存储引用；未设置时为null", nullable = true) String coverImageUrl,
        @Schema(description = "食堂简介；未设置时为null", nullable = true) String description,
        @Schema(description = "纬度；未设置时为null", nullable = true) BigDecimal latitude,
        @Schema(description = "经度；未设置时为null", nullable = true) BigDecimal longitude) {
    public static DiningHallResponse from(DiningHall hall) {
        return new DiningHallResponse(hall.id(), hall.name(), hall.coverImageUrl(), hall.description(),
                hall.latitude(), hall.longitude());
    }
}
