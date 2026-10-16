package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth.password-change")
public record PasswordChangeProperties(@NotNull Duration window, @Min(1) int userLimit,
                                       @Min(1) int ipLimit,
                                       @NotBlank @Pattern(regexp = "[A-Za-z0-9:_-]{1,100}") String redisNamespace) {
    @AssertTrue(message = "Password change window must be whole positive seconds and no more than 30 days")
    public boolean isWindowValid() {
        return window != null && window.getSeconds() > 0 && window.getNano() == 0
                && window.compareTo(Duration.ofDays(30)) <= 0;
    }
}
