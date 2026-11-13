package com.FoodtimeNeo.api.system;

import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.config.properties.AppReleaseProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "基础运行检查")
public class SystemController {
    private final AppReleaseProperties release;

    public SystemController(AppReleaseProperties release) {
        this.release = release;
    }

    @GetMapping("/ping")
    @Operation(summary = "检查 HTTP 服务是否可响应", description = "依赖状态请查看 Actuator readiness 接口")
    public ApiResponse<PingResult> ping() {
        return ApiResponse.success(new PingResult("foodtime-neo-backend", "UP"));
    }

    @GetMapping("/app-release")
    @Operation(summary = "Android 更新检查", description = "versionCode 为 0 表示未配置，客户端视为无需更新")
    public ApiResponse<AppReleaseResponse> appRelease() {
        boolean testing=release.testing();
        String notes=testing&&!release.notes().contains("测试版")?"测试版，可选择稍后更新。\n"+release.notes():release.notes();
        return ApiResponse.success(new AppReleaseResponse(release.versionCode(), release.versionName(),
                release.apkUrl(), notes, testing?0:release.minVersionCode()));
    }

    public record PingResult(String service, String status) { }

    public record AppReleaseResponse(int versionCode, String versionName, String apkUrl, String notes,
                                     int minVersionCode) { }
}
