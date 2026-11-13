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
@ConfigurationProperties(prefix = "app.auth.captcha")
public record CaptchaProperties(
        boolean enabled,
        @NotNull Duration window,
        @Min(1) int triggerAfter,
        @NotNull Duration challengeTtl,
        @NotNull Duration issueWindow,
        @Min(1) int issueDeviceLimit,
        @Min(1) int issueIpLimit,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9:_-]{1,100}") String redisNamespace,
        String secret) {

    @AssertTrue(message = "CAPTCHA durations must be whole positive seconds and no more than 30 days")
    public boolean isDurationsValid() {
        return valid(window) && valid(issueWindow) && inRange(challengeTtl, 30, 300);
    }

    private static boolean valid(Duration value) {
        return value != null && value.getSeconds() >= 1 && value.getNano() == 0
                && value.compareTo(Duration.ofDays(30)) <= 0;
    }

    private static boolean inRange(Duration value, long minSeconds, long maxSeconds) {
        return value != null && value.getNano() == 0
                && value.getSeconds() >= minSeconds && value.getSeconds() <= maxSeconds;
    }
}
