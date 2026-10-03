package com.FoodtimeNeo.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@Schema(description = "注册请求；邮箱将去除首尾空白并转为小写，密码保持原样")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(
        @NotBlank(message = "邮箱不能为空")
        @Size(max = 254, message = "邮箱长度不能超过254位")
        @Pattern(regexp = "\\A[0-9]+@bjtu\\.edu\\.cn\\z", message = "邮箱须为数字加@bjtu.edu.cn")
        @Schema(description = "数字前缀的北京交通大学邮箱", example = "20260001@bjtu.edu.cn")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 128, message = "密码长度须为8至128位")
        @Pattern(regexp = "\\A[!-~]+\\z", message = "密码只能包含英文字母、数字和半角符号，不允许空白或其他字符")
        @Pattern(regexp = "(?s)\\A(?=.*[0-9])(?=.*[A-Za-z]).*\\z", message = "密码须至少包含一位数字和一位英文字母")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(description = "8至128位，只允许英文字母、数字和ASCII半角符号（!至~）；至少包含数字和英文字母，不允许空白",
                accessMode = Schema.AccessMode.WRITE_ONLY, minLength = 8, maxLength = 128)
        String password,

        @NotBlank(message = "邮箱验证码不能为空")
        @Pattern(regexp = "\\A[0-9]{6}\\z", message = "邮箱验证码须为6位数字")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(description = "邮箱收到的6位数字验证码，15分钟内有效",
                accessMode = Schema.AccessMode.WRITE_ONLY, minLength = 6, maxLength = 6, example = "012345")
        String verificationCode) {

    public RegisterRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "RegisterRequest[email=<redacted>, password=<redacted>, verificationCode=<redacted>]";
    }
}
