package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.service.LoginRateLimitException;
import com.FoodtimeNeo.auth.service.PasswordChangeRateLimitException;
import com.FoodtimeNeo.api.user.PasswordChangeController;
import com.FoodtimeNeo.auth.verification.EmailRateLimitException;
import com.FoodtimeNeo.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Keep authentication-specific error codes out of the shared exception layer. */
@RestControllerAdvice(assignableTypes = {LoginController.class, EmailVerificationController.class, PasswordChangeController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler(PasswordChangeRateLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handlePasswordChangeRateLimit(PasswordChangeRateLimitException exception) {
        return ResponseEntity.status(exception.getStatus())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfter()))
                .body(ApiResponse.error(exception.getCode(), exception.getMessage(), null));
    }

    @ExceptionHandler(EmailRateLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailRateLimit(EmailRateLimitException exception) {
        return ResponseEntity.status(exception.getStatus())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfter()))
                .body(ApiResponse.error(exception.getCode(), exception.getMessage(), null));
    }

    @ExceptionHandler(LoginRateLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoginRateLimit(LoginRateLimitException exception) {
        return ResponseEntity.status(exception.getStatus())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfter()))
                .body(ApiResponse.error(exception.getCode(), exception.getMessage(), null));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleSessionFailure(DataAccessException exception) {
        LOG.error("Authentication persistence failed: {}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("AUTH_UNAVAILABLE", "登录服务暂时不可用，请稍后重试", null));
    }
}
