package com.FoodtimeNeo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record EmailCodeRequest(
        @NotBlank(message = "邮箱不能为空") @Size(max = 254, message = "邮箱长度不能超过254位")
        @Pattern(regexp = "\\A[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9-]{1,63}(\\.[A-Za-z0-9-]{1,63})+\\z", message = "邮箱格式不正确")
        @Schema(description = "待注册的邮箱", example = "20260001@bjtu.edu.cn") String email) {
    public EmailCodeRequest { email = email == null ? null : email.strip().toLowerCase(Locale.ROOT); }
    @Override
    public String toString() { return "EmailCodeRequest[email=<redacted>]"; }
}
