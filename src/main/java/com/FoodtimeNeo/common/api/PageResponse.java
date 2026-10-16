package com.FoodtimeNeo.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "分页结果；页码、大小、总数均为查询协议数据")
public record PageResponse<T>(List<T> items, long total, int page, int size) {
    public static <T> PageResponse<T> of(List<T> items, long total, PageRequest request) {
        return new PageResponse<>(items, total, request.page(), request.size());
    }
}
