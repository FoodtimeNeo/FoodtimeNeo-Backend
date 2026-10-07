package com.FoodtimeNeo.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "修改当前登录用户的密码；密码保持原样，不裁剪空白")
public record ChangePasswordRequest(
        @NotBlank(message = "旧密码不能为空")
        @Size(min = 8, max = 128, message = "旧密码长度须为8至128位")
        @Pattern(regexp = "\\A[!-~]+\\z", message = "旧密码只能包含英文字母、数字和半角符号，不允许空白或其他字符")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(description = "当前密码", accessMode = Schema.AccessMode.WRITE_ONLY, minLength = 8, maxLength = 128)
        String oldPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 128, message = "新密码长度须为8至128位")
        @Pattern(regexp = "\\A[!-~]+\\z", message = "新密码只能包含英文字母、数字和半角符号，不允许空白或其他字符")
        @Pattern(regexp = "(?s)\\A(?=.*[0-9])(?=.*[A-Za-z]).*\\z", message = "新密码须至少包含一位数字和一位英文字母")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(description = "新密码：8至128位ASCII字符，至少包含数字和英文字母；不能与旧密码相同",
                accessMode = Schema.AccessMode.WRITE_ONLY, minLength = 8, maxLength = 128)
        String newPassword) {
    @Override
    public String toString() { return "ChangePasswordRequest[oldPassword=<redacted>, newPassword=<redacted>]"; }
}
