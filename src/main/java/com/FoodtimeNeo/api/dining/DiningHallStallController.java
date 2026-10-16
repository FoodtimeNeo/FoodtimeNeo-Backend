package com.FoodtimeNeo.api.dining;

import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.dining.dto.StallResponse;
import com.FoodtimeNeo.dining.service.StallService;
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
@RequestMapping("/api/v1/dining-halls/{diningHallId}/stalls")
@Tag(name = "Dining", description = "食堂与档口展示信息")
public class DiningHallStallController {
    private final StallService stalls;
    public DiningHallStallController(StallService stalls) { this.stalls = stalls; }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "获取食堂下档口列表", description = "需要登录；只展示启用食堂下的启用档口；按排序值、ID升序排列，无档口返回空数组")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "返回档口列表"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "食堂ID格式错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或会话失效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "食堂不存在或已停用"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "数据库或认证基础设施不可用")
    })
    public ApiResponse<List<StallResponse>> list(
            @Parameter(description = "所属食堂UUID", required = true) @PathVariable UUID diningHallId) {
        return ApiResponse.success(stalls.listByDiningHall(diningHallId));
    }
}
