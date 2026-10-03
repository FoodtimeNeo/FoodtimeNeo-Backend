package com.FoodtimeNeo.common.api;

import org.slf4j.MDC;

import java.time.Instant;

public record ApiResponse<T>(String code, String message, T data, Instant timestamp, String requestId) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "success", data, Instant.now(), MDC.get("requestId"));
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(code, message, data, Instant.now(), MDC.get("requestId"));
    }
}
