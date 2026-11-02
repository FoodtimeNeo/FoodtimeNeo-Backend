package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.dto.EmailCodeResponse;
import com.FoodtimeNeo.auth.dto.SchoolEmailCodeRequest;
import com.FoodtimeNeo.auth.security.DeviceIdentityService;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.user.dto.BindSchoolEmailRequest;
import com.FoodtimeNeo.user.dto.ProfileResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.service.SchoolEmailBindingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "SchoolEmail", description = "绑定学校邮箱以解锁发布权限")
@SecurityRequirement(name = "sessionCookie")
public class SchoolEmailController {
    private final SchoolEmailBindingService binding;
    private final DeviceIdentityService devices;

    public SchoolEmailController(SchoolEmailBindingService binding, DeviceIdentityService devices) {
        this.binding = binding;
        this.devices = devices;
    }

    @PostMapping("/auth/bind-email/email-code")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "发送绑定学校邮箱验证码", description = "仅接受数字@bjtu.edu.cn 邮箱；与注册发码共用配额")
    public ApiResponse<EmailCodeResponse> sendCode(@Valid @RequestBody SchoolEmailCodeRequest body,
                                                   HttpServletRequest http, HttpServletResponse response) {
        String deviceId = devices.resolve(http, response);
        return ApiResponse.success(binding.sendCode(current(http), body, http.getRemoteAddr(), deviceId));
    }

    @PostMapping("/users/me/bind-email")
    @Operation(summary = "绑定学校邮箱", description = "验证码通过后访客升级为普通用户")
    public ApiResponse<ProfileResponse> bind(@Valid @RequestBody BindSchoolEmailRequest body,
                                             HttpServletRequest request) {
        return ApiResponse.success(binding.bind(current(request), body));
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
