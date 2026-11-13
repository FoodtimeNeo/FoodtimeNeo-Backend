package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.dto.RegistrationPolicyResponse;
import com.FoodtimeNeo.auth.service.RegistrationPolicyService;
import com.FoodtimeNeo.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "注册能力")
public class RegistrationPolicyController {
    private final RegistrationPolicyService policies;

    public RegistrationPolicyController(RegistrationPolicyService policies) {
        this.policies = policies;
    }

    @GetMapping("/registration-policy")
    @Operation(summary = "获取注册策略",
            description = "公开当前注册模式、是否可发送邮箱验证码、是否必须邀请码及策略版本；不包含限流值、邀请列表或账户状态")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "策略读取成功")
    })
    public ApiResponse<RegistrationPolicyResponse> policy() {
        return ApiResponse.success(policies.current());
    }
}
