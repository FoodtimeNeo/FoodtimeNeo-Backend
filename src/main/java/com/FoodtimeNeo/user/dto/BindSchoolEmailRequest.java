package com.FoodtimeNeo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/** 校验学校邮箱验证码并完成绑定。 */
@Schema(description = "绑定学校邮箱；验证通过后访客升级为普通用户")
public record BindSchoolEmailRequest(
        @NotBlank(message = "学校邮箱不能为空")
        @Size(max = 254, message = "邮箱长度不能超过254位")
        @Pattern(regexp = "\\A[0-9]+@bjtu\\.edu\\.cn\\z", message = "学校邮箱须为数字加@bjtu.edu.cn")
        String email,
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "\\A[0-9]{6}\\z", message = "验证码须为6位数字")
        String verificationCode) {
    public BindSchoolEmailRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
