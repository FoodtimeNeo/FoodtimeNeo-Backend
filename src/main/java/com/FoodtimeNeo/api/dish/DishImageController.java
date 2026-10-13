package com.FoodtimeNeo.api.dish;

import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.dish.dto.DishImageResponse;
import com.FoodtimeNeo.dish.service.DishImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dishes/{dishId}/images")
@Tag(name = "Dishes", description = "菜品展示信息")
public class DishImageController {
    private final DishImageService images;
    public DishImageController(DishImageService images) { this.images = images; }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "获取菜品图册", description = "需要登录；分页展示启用食堂、档口、菜品下已通过审核的图片；按创建时间倒序、ID升序排列")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "返回分页图册，无图片或超出末页时items为空"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "菜品ID或分页参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或会话失效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "菜品不存在或三级状态不可见"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "查询或认证服务不可用")
    })
    public ApiResponse<PageResponse<DishImageResponse>> list(
            @Parameter(description = "菜品UUID", required = true) @PathVariable UUID dishId,
            @Parameter(description = "页码，从1开始", schema = @Schema(minimum = "1")) @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，1到100", schema = @Schema(minimum = "1", maximum = "100")) @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(images.list(dishId, new PageRequest(page, size)));
    }
}
