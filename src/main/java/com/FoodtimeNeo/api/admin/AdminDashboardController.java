package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.dashboard.DashboardDtos;
import com.FoodtimeNeo.dashboard.DashboardService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 平台数据看板：仅平台管理员（superadmin）可读，普通管理员无权。
 * 口径定义见 docs/admin-dashboard.md。
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@SecurityRequirement(name = "sessionCookie")
public class AdminDashboardController {
    private final DashboardService dashboard;

    public AdminDashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    @Operation(summary = "平台数据看板", description = "累计指标 + 按日趋势；days 取值 1 到 31，默认 7")
    public ApiResponse<DashboardDtos.DashboardResponse> overview(HttpServletRequest request,
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(dashboard.overview(current(request), days));
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
