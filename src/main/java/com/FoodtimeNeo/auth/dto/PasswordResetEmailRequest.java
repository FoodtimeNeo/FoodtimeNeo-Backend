package com.FoodtimeNeo.auth.dto;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record PasswordResetEmailRequest(
        @NotBlank @Size(max=254) @Email @Pattern(regexp="\\A[^\\s@]+@[^\\s@]+\\.[^\\s@]+\\z", message="请输入完整邮箱") String email) {
    public PasswordResetEmailRequest { email=email==null?null:email.strip().toLowerCase(Locale.ROOT); }
    @Override public String toString() { return "PasswordResetEmailRequest[email=<redacted>]"; }
}
