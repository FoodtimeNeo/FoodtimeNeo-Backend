package com.FoodtimeNeo.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(@NotNull Duration sessionTtl, boolean cookieSecure,
                             @NotNull Duration loginWindow, @Min(1) int loginAccountLimit,
                             @Min(1) int loginIpLimit,
                             @NotBlank @Pattern(regexp = "[A-Za-z0-9:_-]{1,100}") String redisNamespace) {
    @AssertTrue(message = "Authentication durations must be whole positive seconds and no more than 30 days")
    public boolean isDurationsValid() {
        return valid(sessionTtl) && valid(loginWindow);
    }

    private static boolean valid(Duration value) {
        return value != null && value.getSeconds() >= 1 && value.getNano() == 0
                && value.compareTo(Duration.ofDays(30)) <= 0;
    }
}
