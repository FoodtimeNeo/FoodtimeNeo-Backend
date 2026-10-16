package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.dto.ChangePasswordRequest;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.auth.service.PasswordChangeRateLimiter;
import com.FoodtimeNeo.auth.service.PasswordChangeService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "User", description = "当前用户账号管理")
public class PasswordChangeController {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordChangeController.class);
    private final PasswordChangeService changes;
    private final PasswordChangeRateLimiter limiter;
    private final SessionAuthenticationService sessions;
    private final CookieSerializer cookies;
    public PasswordChangeController(PasswordChangeService changes, PasswordChangeRateLimiter limiter,
                                    SessionAuthenticationService sessions, CookieSerializer cookies) {
        this.changes = changes;
        this.limiter = limiter;
        this.sessions = sessions;
        this.cookies = cookies;
    }
    @PutMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "sessionCookie")
    @Operation(summary = "修改当前用户密码", description = "需要登录Cookie和CSRF令牌；验证旧密码并更新新密码，成功后必须重新登录，所有旧会话失效")
    public ApiResponse<Void> change(@Valid @RequestBody ChangePasswordRequest body,
                                    HttpServletRequest request, HttpServletResponse response) {
        UserProfile current = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        limiter.check(current.id(), request.getRemoteAddr());
        changes.change(current, body);
        try {
            sessions.clear(request);
        } catch (DataAccessException exception) {
            // Already committed: password-version checks reject old sessions even if Redis deletion fails.
            LOG.error("Password changed; session cleanup failed: {}", exception.getClass().getSimpleName());
        } finally {
            SecurityContextHolder.clearContext();
            var expired = new CookieSerializer.CookieValue(request, response, "");
            expired.setCookieMaxAge(0);
            cookies.writeCookieValue(expired);
        }
        return ApiResponse.success(null);
    }
}
