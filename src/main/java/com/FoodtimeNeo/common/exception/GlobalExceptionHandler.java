package com.FoodtimeNeo.common.exception;

import com.FoodtimeNeo.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(exception.getCode(), exception.getMessage(), null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", "请求参数校验失败", null));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return new ResponseEntity<>(ApiResponse.error("VALIDATION_ERROR", "请求参数校验失败", errors), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String code;
        String message;
        switch (status.value()) {
            case 400 -> { code = "BAD_REQUEST"; message = "请求参数或请求格式错误"; }
            case 404 -> { code = "NOT_FOUND"; message = "请求的资源不存在"; }
            case 405 -> { code = "METHOD_NOT_ALLOWED"; message = "不支持的请求方法"; }
            case 413 -> { code = "PAYLOAD_TOO_LARGE"; message = "上传内容超出大小限制"; }
            case 415 -> { code = "UNSUPPORTED_MEDIA_TYPE"; message = "不支持的内容类型"; }
            default -> {
                code = "HTTP_" + status.value();
                message = status.is5xxServerError() ? "服务器内部错误" : "请求无法处理";
            }
        }
        if (status.is5xxServerError()) {
            LOG.error("Request processing failed", exception);
        }
        return super.handleExceptionInternal(exception, ApiResponse.error(code, message, null), headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        LOG.error("Unexpected request processing failure", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "服务器内部错误", null));
    }

    public record FieldError(String field, String message) { }
}
