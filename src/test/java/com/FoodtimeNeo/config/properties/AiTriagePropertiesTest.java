package com.FoodtimeNeo.config.properties;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AiTriagePropertiesTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private AiTriageProperties config(boolean enabled, String provider, String baseUrl, String key, String model) {
        return new AiTriageProperties(enabled, provider, baseUrl, key, model,
                "escalation-model", "fallback-model", 0.9, "photo-model", 0.95, 0.9,
                Duration.ofSeconds(30), 10, Duration.ofMinutes(5), Duration.ofMinutes(1));
    }

    @Test
    void disabledNeedsNothing() {
        assertThat(validator.validate(config(false, "openai", "", "", ""))).isEmpty();
    }

    @Test
    void fakeProviderNeedsNothingEvenWhenEnabled() {
        assertThat(validator.validate(config(true, "fake", "", "", ""))).isEmpty();
    }

    @Test
    void enabledOpenaiRequiresEndpointKeyAndModel() {
        assertThat(validator.validate(config(true, "openai", "", "secret", "model")))
                .anyMatch(v -> v.getMessage().contains("base-url"));
        assertThat(validator.validate(config(true, "openai", "https://api.example.com", "", "model")))
                .anyMatch(v -> v.getMessage().contains("api-key"));
        assertThat(validator.validate(config(true, "openai", "https://api.example.com", "secret", "model"))).isEmpty();
    }

    @Test
    void windowsAndBatchesAreBounded() {
        var bad = new AiTriageProperties(true, "fake", "", "", "", "", "", -0.5, "", 1.5, -0.1,
                Duration.ofSeconds(0), 0, Duration.ofDays(2), Duration.ofSeconds(-1));
        assertThat(validator.validate(bad)).hasSizeGreaterThanOrEqualTo(4);
    }
}
