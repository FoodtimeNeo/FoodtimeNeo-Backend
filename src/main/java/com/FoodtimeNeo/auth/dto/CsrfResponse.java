package com.FoodtimeNeo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "CSRF令牌；写请求需要在指定headerName请求头携带token")
public record CsrfResponse(String headerName, String token) {
    @Override
    public String toString() { return "CsrfResponse[headerName=" + headerName + ", token=<redacted>]"; }
}
