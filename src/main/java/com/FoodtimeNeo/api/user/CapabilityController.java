package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.authz.CapabilityService;
import com.FoodtimeNeo.authz.CapabilitySnapshot;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Capabilities", description = "当前账号的有效能力")
public class CapabilityController {
    private final CapabilityService capabilities;

    public CapabilityController(CapabilityService capabilities) {
        this.capabilities = capabilities;
    }

    @GetMapping("/capabilities")
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "获取当前有效能力",
            description = "返回服务器判定的稳定能力名与策略版本；客户端只据此显示动作，写请求仍由服务器逐次校验")
    public ApiResponse<CapabilitySnapshot> current(HttpServletRequest request) {
        UserProfile actor = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(capabilities.snapshot(actor));
    }
}
