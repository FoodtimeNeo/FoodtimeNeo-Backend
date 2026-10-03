package com.FoodtimeNeo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record EmailCodeRequest(
        @NotBlank(message = "邮箱不能为空") @Size(max = 254, message = "邮箱长度不能超过254位")
        @Pattern(regexp = "\\A[0-9]+@bjtu\\.edu\\.cn\\z", message = "邮箱须为数字加@bjtu.edu.cn")
        @Schema(description = "待注册的校园邮箱", example = "20260001@bjtu.edu.cn") String email) {
    public EmailCodeRequest { email = email == null ? null : email.strip().toLowerCase(Locale.ROOT); }
    @Override
    public String toString() { return "EmailCodeRequest[email=<redacted>]"; }
}
