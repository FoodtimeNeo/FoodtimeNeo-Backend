package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.captcha.CaptchaService;
import com.FoodtimeNeo.auth.dto.CaptchaResponse;
import com.FoodtimeNeo.auth.security.DeviceIdentityService;
import com.FoodtimeNeo.auth.service.CaptchaRateLimiter;
import com.FoodtimeNeo.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "登录验证码")
public class CaptchaController {
    private final CaptchaService captcha;
    private final CaptchaRateLimiter limits;
    private final DeviceIdentityService devices;

    public CaptchaController(CaptchaService captcha, CaptchaRateLimiter limits, DeviceIdentityService devices) {
        this.captcha = captcha;
        this.limits = limits;
        this.devices = devices;
    }

    @GetMapping("/captcha")
    @Operation(summary = "获取登录算术图片验证码",
            description = "动态生成加减乘除中的加/乘/整除题；2分钟有效、一次性、绑定设备；响应禁止缓存")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "挑战已生成"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "验证码未启用"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "取题过于频繁")
    })
    public ApiResponse<CaptchaResponse> captcha(HttpServletRequest request, HttpServletResponse response) {
        String deviceId = devices.resolve(request, response);
        limits.check(deviceId, request.getRemoteAddr());
        var issued = captcha.issue(deviceId);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        return ApiResponse.success(new CaptchaResponse(issued.challengeId(),
                Base64.getEncoder().encodeToString(issued.image()), issued.expiresAt()));
    }
}
