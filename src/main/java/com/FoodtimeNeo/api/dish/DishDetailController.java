package com.FoodtimeNeo.api.dish;

import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.dish.dto.DishDetailResponse;
import com.FoodtimeNeo.dish.service.DishDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dishes/{dishId}")
@Tag(name = "Dishes", description = "菜品展示信息")
public class DishDetailController {
    private final DishDetailService dishes;
    public DishDetailController(DishDetailService dishes) { this.dishes = dishes; }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "获取菜品基础详情", description = "需要登录；检查食堂、档口、菜品三级启用状态；返回基础信息和所属关系，不包含图片、评分或评论")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "返回菜品基础详情"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "菜品ID格式错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或会话失效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "菜品不存在或食堂、档口、菜品已停用"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "查询或认证服务不可用")
    })
    public ApiResponse<DishDetailResponse> get(
            @Parameter(description = "菜品UUID", required = true) @PathVariable UUID dishId) {
        return ApiResponse.success(dishes.get(dishId));
    }
}
