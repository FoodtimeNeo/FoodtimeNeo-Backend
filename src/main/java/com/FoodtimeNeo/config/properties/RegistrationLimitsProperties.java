package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth.registration")
public record RegistrationLimitsProperties(@NotNull Duration window, @Min(1) int accountLimit,
        @Min(1) int ipLimit, @NotBlank @Pattern(regexp = "[A-Za-z0-9:_-]{1,100}") String redisNamespace) {
    @AssertTrue(message = "Registration window must be whole positive seconds and no more than 30 days")
    public boolean isWindowValid() {
        return window != null && window.getSeconds() > 0 && window.getNano() == 0
                && window.compareTo(Duration.ofDays(30)) <= 0;
    }
}
