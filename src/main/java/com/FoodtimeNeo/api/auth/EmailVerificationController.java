package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.dto.EmailCodeRequest;
import com.FoodtimeNeo.auth.dto.EmailCodeResponse;
import com.FoodtimeNeo.auth.verification.EmailVerificationService;
import com.FoodtimeNeo.auth.security.DeviceIdentityService;
import com.FoodtimeNeo.auth.service.RegistrationGate;
import com.FoodtimeNeo.auth.service.RegistrationRateLimiter;
import com.FoodtimeNeo.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class EmailVerificationController {
    private final EmailVerificationService verification;
    private final RegistrationRateLimiter limits;
    private final DeviceIdentityService devices;
    private final RegistrationGate gate;
    private final com.FoodtimeNeo.user.lifecycle.EmailCooldowns cooldowns;
    public EmailVerificationController(EmailVerificationService verification, RegistrationRateLimiter limits,
                                       DeviceIdentityService devices, RegistrationGate gate,com.FoodtimeNeo.user.lifecycle.EmailCooldowns cooldowns) {
        this.verification = verification; this.limits = limits; this.devices = devices; this.gate = gate;
        this.cooldowns=cooldowns;
    }

    @PostMapping(value = "/register/email-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "发送注册邮箱验证码", description = "发送6位数字验证码，15分钟有效；需要CSRF令牌，限制重发及邮箱/IP频率")
    public ApiResponse<EmailCodeResponse> send(@Valid @RequestBody EmailCodeRequest request,
                                               HttpServletRequest http, HttpServletResponse response) {
        // A closed door must not keep sending mail through the shared SMTP account.
        gate.requireAcceptingRequests();
        gate.requireAllowedEmail(request.email());
        cooldowns.requireAvailable(request.email());
        String deviceId = devices.resolve(http, response);
        limits.check(request.email(), http.getRemoteAddr(), deviceId);
        return ApiResponse.success(verification.send(request, http.getRemoteAddr(), deviceId));
    }
}
