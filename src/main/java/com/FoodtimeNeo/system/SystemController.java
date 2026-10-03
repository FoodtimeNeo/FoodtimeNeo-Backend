package com.FoodtimeNeo.system;

import com.FoodtimeNeo.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "基础运行检查")
public class SystemController {
    @GetMapping("/ping")
    @Operation(summary = "检查 HTTP 服务是否可响应", description = "依赖状态请查看 Actuator readiness 接口")
    public ApiResponse<PingResult> ping() {
        return ApiResponse.success(new PingResult("foodtime-neo-backend", "UP"));
    }

    public record PingResult(String service, String status) { }
}
