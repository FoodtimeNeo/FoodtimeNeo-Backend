package com.FoodtimeNeo.api.dish;

import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.dish.dto.DishSearchResponse;
import com.FoodtimeNeo.dish.service.DishSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dishes/search")
@SecurityRequirement(name = "sessionCookie")
public class DishSearchController {
    private final DishSearchService dishes;

    public DishSearchController(DishSearchService dishes) {
        this.dishes = dishes;
    }

    @GetMapping
    @Operation(summary = "跨食堂搜索与筛选菜品",
            description = "仅返回启用食堂、档口下的启用菜品；q 可空。支持食堂多选、价格区间、最低评分与白名单排序；价格或评分未知的菜品保留并排在完整数据之后。")
    public ApiResponse<PageResponse<DishSearchResponse>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) List<UUID> diningHallIds,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(dishes.search(q, diningHallIds, minPrice, maxPrice, minRating,
                sort, new PageRequest(page, size)));
    }
}
