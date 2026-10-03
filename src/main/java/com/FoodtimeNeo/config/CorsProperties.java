package com.FoodtimeNeo.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @NotNull List<String> allowedOrigins,
        @NotNull List<String> allowedMethods,
        @NotNull List<String> allowedHeaders,
        @NotNull List<String> exposedHeaders,
        @NotNull Duration maxAge) {

    @AssertTrue(message = "CORS origins must be explicit http(s) origins without a path or wildcard")
    public boolean isOriginsValid() {
        if (allowedOrigins == null) {
            return false;
        }
        return allowedOrigins.stream().allMatch(origin -> {
            try {
                URI uri = URI.create(origin);
                return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                        && uri.getHost() != null && uri.getUserInfo() == null
                        && uri.getRawQuery() == null && uri.getRawFragment() == null
                        && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                        && uri.getPort() >= -1 && uri.getPort() <= 65535;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        });
    }

    @AssertTrue(message = "CORS max-age must be non-negative")
    public boolean isMaxAgeValid() {
        return maxAge != null && !maxAge.isNegative();
    }
}
