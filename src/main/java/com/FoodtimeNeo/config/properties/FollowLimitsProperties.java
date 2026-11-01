package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.follow.actions")
public record FollowLimitsProperties(@NotNull Duration window, @Min(1) int userLimit, @Min(1) int ipLimit,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9:_-]{1,100}") String redisNamespace) {
    @AssertTrue(message = "Follow window must be whole positive seconds and no more than 30 days")
    public boolean isWindowValid() {
        return window != null && window.getSeconds() > 0 && window.getNano() == 0
                && window.compareTo(Duration.ofDays(30)) <= 0;
    }
}
