package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.dto.CsrfResponse;
import com.FoodtimeNeo.auth.dto.LoginRequest;
import com.FoodtimeNeo.auth.dto.LoginResponse;
import com.FoodtimeNeo.auth.dto.UserResponse;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.auth.service.LoginRateLimiter;
import com.FoodtimeNeo.auth.service.LoginService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "账号与登录会话")
public class LoginController {
    private final LoginService logins;
    private final LoginRateLimiter limiter;
    private final SessionAuthenticationService sessions;

    public LoginController(LoginService logins, LoginRateLimiter limiter, SessionAuthenticationService sessions) {
        this.logins = logins;
        this.limiter = limiter;
        this.sessions = sessions;
    }

    @GetMapping("/csrf")
    @Operation(summary = "获取CSRF令牌", description = "登录前及登录成功后获取；写请求携带返回的请求头和Cookie")
    public ApiResponse<CsrfResponse> csrf(CsrfToken token) {
        return ApiResponse.success(new CsrfResponse(token.getHeaderName(), token.getToken()));
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "邮箱密码登录", description = "验证成功设置HttpOnly Cookie；每次登录创建新会话，固定期限7天（可配置）")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest login,
                                            HttpServletRequest request, HttpServletResponse response) {
        limiter.check(login.account(), request.getRemoteAddr());
        UserProfile user = logins.authenticate(login);
        return ApiResponse.success(new LoginResponse(UserResponse.from(user), sessions.establish(user, request, response)));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "获取当前登录用户", description = "校验会话、账号状态和密码变更；未登录或已失效返回401")
    public ApiResponse<UserResponse> me(HttpServletRequest request) {
        return ApiResponse.success(UserResponse.from((UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER)));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "退出当前会话", description = "需要有效Cookie和CSRF令牌；删除Redis会话并清除Cookie")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        sessions.clear(request);
        return ApiResponse.success(null);
    }
}
