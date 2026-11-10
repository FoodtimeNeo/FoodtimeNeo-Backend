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

@RestController
@RequestMapping("/api/v1/dishes/search")
@SecurityRequirement(name = "sessionCookie")
public class DishSearchController {
    private final DishSearchService dishes;

    public DishSearchController(DishSearchService dishes) {
        this.dishes = dishes;
    }

    @GetMapping
    @Operation(summary = "跨食堂按名称搜索菜品", description = "仅返回启用食堂、档口下的启用菜品。完全匹配优先，其次为前缀匹配。")
    public ApiResponse<PageResponse<DishSearchResponse>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(dishes.search(q, new PageRequest(page, size)));
    }
}
