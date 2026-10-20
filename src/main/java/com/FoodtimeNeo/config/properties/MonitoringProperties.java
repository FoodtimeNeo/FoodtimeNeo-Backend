package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.monitoring")
public record MonitoringProperties(
        @Pattern(regexp = "[0-9a-fA-F.:/]{0,45}", message = "Scrape address must be an IP or CIDR range")
        String scrapeIp,
        @NotNull Duration slowRequestThreshold) {
    @AssertTrue(message = "Slow request threshold must be whole seconds between 0 and 300")
    public boolean isThresholdValid() {
        return slowRequestThreshold != null && slowRequestThreshold.getNano() == 0
                && !slowRequestThreshold.isNegative()
                && slowRequestThreshold.compareTo(Duration.ofSeconds(300)) <= 0;
    }
}
