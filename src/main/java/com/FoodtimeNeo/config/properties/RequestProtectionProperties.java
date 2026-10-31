package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Deployment-wide in-flight request protection. The global semaphore keeps Tomcat threads and
 * memory bounded; the per-path semaphores keep SMTP and Argon2 work below the server's safe
 * concurrency. These are concurrency caps, not per-user quotas.
 */
@Validated
@ConfigurationProperties(prefix = "app.protection")
public record RequestProtectionProperties(
        boolean enabled,
        @Min(1) int globalMaxConcurrency,
        @Min(1) int emailCodeMaxConcurrency,
        @Min(1) int registrationMaxConcurrency,
        @Min(1) int loginMaxConcurrency,
        @Min(1) @Max(60) int retryAfterSeconds) {

    @AssertTrue(message = "Per-path concurrency limits must not exceed the global limit")
    public boolean isPerPathLimitsWithinGlobal() {
        return emailCodeMaxConcurrency <= globalMaxConcurrency
                && registrationMaxConcurrency <= globalMaxConcurrency
                && loginMaxConcurrency <= globalMaxConcurrency;
    }
}
