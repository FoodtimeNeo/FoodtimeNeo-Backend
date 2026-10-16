package com.FoodtimeNeo.api.dining;

import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.dining.dto.DiningHallResponse;
import com.FoodtimeNeo.dining.service.DiningHallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dining-halls")
@Tag(name = "Dining", description = "食堂展示信息")
public class DiningHallController {
    private final DiningHallService halls;
    public DiningHallController(DiningHallService halls) { this.halls = halls; }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "获取食堂列表", description = "需要登录；返回全部启用食堂，按排序值、ID升序排列；无数据返回空数组")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "返回食堂列表"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录或会话失效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "数据库或认证基础设施不可用")
    })
    public ApiResponse<List<DiningHallResponse>> list() { return ApiResponse.success(halls.list()); }
}
