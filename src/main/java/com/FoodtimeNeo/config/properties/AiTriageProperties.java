package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.ai.triage")
public record AiTriageProperties(boolean enabled,
        @Pattern(regexp = "fake|openai", message = "AI provider must be fake or openai") String provider,
        String baseUrl, String apiKey, String primaryModel, String escalationModel, String fallbackModel,
        @DecimalMin("0.0") @DecimalMax("1.0") double escalationBelowConfidence, String photoModel,
        @DecimalMin("0.0") @DecimalMax("1.0") double autoApproveAbove,
        @DecimalMin("0.0") @DecimalMax("1.0") double autoRejectAbove,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9:_-]{1,100}", message = "AI triage redis namespace has illegal characters")
        String redisNamespace,
        @NotNull Duration timeout, @Min(1) @Max(100) int batchSize,
        @NotNull Duration interval, @NotNull Duration initialDelay) {
    @AssertTrue(message = "AI triage timeout must be whole positive seconds and no more than 120")
    public boolean isTimeoutValid() {
        return timeout != null && timeout.getSeconds() > 0 && timeout.getNano() == 0
                && timeout.compareTo(Duration.ofSeconds(120)) <= 0;
    }

    @AssertTrue(message = "AI triage interval must be whole positive seconds and no more than one day")
    public boolean isIntervalValid() {
        return interval != null && interval.getSeconds() > 0 && interval.getNano() == 0
                && interval.compareTo(Duration.ofDays(1)) <= 0;
    }

    @AssertTrue(message = "AI triage initial delay must be whole non-negative seconds and no more than one day")
    public boolean isInitialDelayValid() {
        return initialDelay != null && initialDelay.getNano() == 0 && !initialDelay.isNegative()
                && initialDelay.compareTo(Duration.ofDays(1)) <= 0;
    }

    @AssertFalse(message = "The openai provider requires base-url, api-key, primary-model and photo-model")
    public boolean isOpenaiIncomplete() {
        return enabled && "openai".equals(provider)
                && (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()
                        || primaryModel == null || primaryModel.isBlank()
                        || photoModel == null || photoModel.isBlank());
    }
}
