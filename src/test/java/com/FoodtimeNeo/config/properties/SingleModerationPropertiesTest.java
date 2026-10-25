package com.FoodtimeNeo.config.properties;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SingleModerationPropertiesTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private SingleModerationProperties config(boolean enabled, String baseUrl, String key, String model) {
        return new SingleModerationProperties(enabled, baseUrl, key, model, Duration.ofSeconds(30));
    }

    @Test
    void disabledNeedsNothing() {
        assertThat(validator.validate(config(false, "", "", ""))).isEmpty();
    }

    @Test
    void enabledRequiresEndpointKeyAndModel() {
        assertThat(validator.validate(config(true, "", "secret", "model")))
                .anyMatch(v -> v.getMessage().contains("base-url"));
        assertThat(validator.validate(config(true, "https://api.example.com", "", "model")))
                .anyMatch(v -> v.getMessage().contains("api-key"));
        assertThat(validator.validate(config(true, "https://api.example.com", "secret", "")))
                .anyMatch(v -> v.getMessage().contains("model"));
        assertThat(validator.validate(config(true, "https://api.example.com", "secret", "model"))).isEmpty();
    }

    @Test
    void rejectsCommaSeparatedKeys() {
        assertThat(validator.validate(config(true, "https://api.example.com", "key1,key2", "model")))
                .anyMatch(v -> v.getMessage().contains("exactly one"));
        assertThat(validator.validate(config(true, "https://api.example.com", "single-key", "model"))).isEmpty();
    }

    @Test
    void timeoutIsBounded() {
        var bad = new SingleModerationProperties(true, "https://api.example.com", "secret", "model",
                Duration.ofSeconds(0));
        assertThat(validator.validate(bad)).isNotEmpty();
    }
}
