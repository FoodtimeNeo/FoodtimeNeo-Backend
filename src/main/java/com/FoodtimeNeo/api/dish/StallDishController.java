package com.FoodtimeNeo.api.dish;

import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.dish.dto.DishListItemResponse;
import com.FoodtimeNeo.dish.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stalls/{stallId}/dishes")
@Tag(name = "Dishes", description = "菜品展示信息")
public class StallDishController {
    private final DishService dishes;
    public StallDishController(DishService dishes) { this.dishes = dishes; }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "获取档口下菜品列表", description = "需要登录；只展示启用食堂、档口下的启用菜品；按创建时间、ID升序排列，无菜品返回空数组")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "返回菜品列表"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "档口ID格式错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或会话失效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "档口不存在、已停用或所属食堂已停用"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "数据库或认证基础设施不可用")
    })
    public ApiResponse<List<DishListItemResponse>> list(
            @Parameter(description = "所属档口UUID", required = true) @PathVariable UUID stallId) {
        return ApiResponse.success(dishes.listByStall(stallId));
    }
}
