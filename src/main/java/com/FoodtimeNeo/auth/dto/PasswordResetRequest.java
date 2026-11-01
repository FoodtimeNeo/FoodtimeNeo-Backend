package com.FoodtimeNeo.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

/** No email/userId/verified parameter can select the password owner. */
public record PasswordResetRequest(
        @NotBlank @Pattern(regexp="\\A[A-Za-z0-9_-]{43}\\z",message="重置凭证无效，请重新验证邮箱")
        @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) String resetToken,
        @NotBlank @Size(min=8,max=128)
        @Pattern(regexp="\\A[!-~]+\\z",message="密码仅允许ASCII可见字符")
        @Pattern(regexp="(?s)\\A(?=.*[0-9])(?=.*[A-Za-z]).*\\z",message="密码须包含字母和数字")
        @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) String newPassword) {
    @Override public String toString() { return "PasswordResetRequest[credentials=<redacted>]"; }
}
