package com.FoodtimeNeo.dining.dto;

import com.FoodtimeNeo.dining.entity.Stall;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "启用档口的展示信息")
public record StallResponse(
        @Schema(description = "档口ID", format = "uuid") UUID id,
        @Schema(description = "所属食堂ID", format = "uuid") UUID diningHallId,
        @Schema(description = "档口名称") String name,
        @Schema(description = "封面图片地址或存储引用；未设置时为null", nullable = true) String coverImageUrl,
        @Schema(description = "档口简介；未设置时为null", nullable = true) String description,
        @Schema(description = "档口图集，按 sort_order、ID 升序排列，从首图开始；无图时为空数组") List<StallImageResponse> images) {
    public static StallResponse from(Stall stall) {
        return from(stall, List.of());
    }

    public static StallResponse from(Stall stall, List<StallImageResponse> images) {
        return new StallResponse(stall.id(), stall.diningHallId(), stall.name(), stall.coverImageUrl(),
                stall.description(), images);
    }
}
