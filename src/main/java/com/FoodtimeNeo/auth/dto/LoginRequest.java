package com.FoodtimeNeo.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@Schema(description = "邮箱账号和密码；账号规范化，密码保持原样")
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginRequest(
        @NotBlank(message = "账号不能为空")
        @Size(max = 254, message = "账号长度不能超过254位")
        @Email(message = "账号须为有效邮箱")
        @Schema(description = "注册时的完整邮箱", example = "20260001@bjtu.edu.cn") String account,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 128, message = "密码长度须为8至128位")
        @Pattern(regexp = "\\A[!-~]+\\z", message = "密码只能包含英文字母、数字和半角符号，不允许空白或其他字符")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(description = "原密码，8至128位英文字母、数字和ASCII半角符号，不允许空白",
                accessMode = Schema.AccessMode.WRITE_ONLY, minLength = 8, maxLength = 128) String password) {
    public LoginRequest {
        account = account == null ? null : account.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "LoginRequest[account=<redacted>, password=<redacted>]";
    }
}
