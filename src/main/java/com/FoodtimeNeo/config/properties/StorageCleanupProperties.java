package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageCleanupProperties(boolean cleanupEnabled, @NotNull Duration cleanupInterval,
                                       @NotNull Duration cleanupMaxAge, @Min(0) long diskWarnBytes) {
    @AssertTrue(message = "Storage cleanup interval must be whole positive seconds and no more than 30 days")
    public boolean isIntervalValid() {
        return cleanupInterval != null && cleanupInterval.getSeconds() > 0 && cleanupInterval.getNano() == 0
                && cleanupInterval.compareTo(Duration.ofDays(30)) <= 0;
    }

    @AssertTrue(message = "Storage cleanup max age must be whole seconds between 0 and 90 days")
    public boolean isMaxAgeValid() {
        return cleanupMaxAge != null && cleanupMaxAge.getNano() == 0 && !cleanupMaxAge.isNegative()
                && cleanupMaxAge.compareTo(Duration.ofDays(90)) <= 0;
    }
}
