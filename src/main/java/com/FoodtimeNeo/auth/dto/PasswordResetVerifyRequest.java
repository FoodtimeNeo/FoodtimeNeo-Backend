package com.FoodtimeNeo.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.util.Locale;

public record PasswordResetVerifyRequest(
        @NotBlank @Size(max=254) @Email @Pattern(regexp="\\A[^\\s@]+@[^\\s@]+\\.[^\\s@]+\\z") String email,
        @NotBlank @Pattern(regexp="\\A[0-9]{6}\\z",message="验证码须为6位数字")
        @JsonProperty(access=JsonProperty.Access.WRITE_ONLY) String verificationCode) {
    public PasswordResetVerifyRequest { email=email==null?null:email.strip().toLowerCase(Locale.ROOT); }
    @Override public String toString() { return "PasswordResetVerifyRequest[credentials=<redacted>]"; }
}
