package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.dto.EmailCodeRequest;
import com.FoodtimeNeo.auth.dto.EmailCodeResponse;
import com.FoodtimeNeo.auth.verification.EmailVerificationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class EmailVerificationController {
    private final EmailVerificationService verification;
    public EmailVerificationController(EmailVerificationService verification) { this.verification = verification; }

    @PostMapping(value = "/register/email-code", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "发送注册邮箱验证码", description = "发送6位数字验证码，15分钟有效；需要CSRF令牌，限制重发及邮箱/IP频率")
    public ApiResponse<EmailCodeResponse> send(@Valid @RequestBody EmailCodeRequest request, HttpServletRequest http) {
        return ApiResponse.success(verification.send(request, http.getRemoteAddr()));
    }
}
