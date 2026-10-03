package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(boolean enabled, @Email String from, String secret,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9:_-]{1,100}") String redisNamespace,
        @NotNull Duration resendInterval, @NotNull Duration sendWindow,
        @Min(1) int sendAccountLimit, @Min(1) int sendIpLimit) {
    public static final Duration CODE_TTL = Duration.ofMinutes(15);
    public static final int MAX_ATTEMPTS = 5;
    public static final int CLAIM_SECONDS = 60;

    @AssertTrue(message = "Enabled email verification requires a sender and a secret of at least 32 characters")
    public boolean isSenderConfigurationValid() {
        return !enabled || (from != null && !from.isBlank() && secret != null && secret.length() >= 32);
    }

    @AssertTrue(message = "Email rate-limit durations must be positive whole seconds, between 60 seconds and one day")
    public boolean isDurationsValid() {
        return valid(resendInterval) && valid(sendWindow);
    }

    private static boolean valid(Duration value) {
        return value != null && value.getNano() == 0 && value.getSeconds() >= 60
                && value.compareTo(Duration.ofDays(1)) <= 0;
    }

    @Override
    public String toString() { return "EmailVerificationProperties[enabled=" + enabled + ", credentials=<redacted>]"; }
}
