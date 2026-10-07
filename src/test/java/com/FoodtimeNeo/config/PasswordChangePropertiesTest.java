package com.FoodtimeNeo.config;

import com.FoodtimeNeo.config.properties.PasswordChangeProperties;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;

class PasswordChangePropertiesTest {
    @Test
    void validatesLimitsWindowAndNamespace() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new PasswordChangeProperties(Duration.ofMinutes(15), 5, 50, "test:password"))).isEmpty();
            for (Duration window : new Duration[] {Duration.ZERO, Duration.ofMillis(1500), Duration.ofDays(31)}) {
                assertThat(validator.validate(new PasswordChangeProperties(window, 5, 50, "test:password"))).isNotEmpty();
            }
            assertThat(validator.validate(new PasswordChangeProperties(Duration.ofMinutes(15), 0, 50, "test:password"))).isNotEmpty();
            assertThat(validator.validate(new PasswordChangeProperties(Duration.ofMinutes(15), 5, 0, "test:password"))).isNotEmpty();
            assertThat(validator.validate(new PasswordChangeProperties(Duration.ofMinutes(15), 5, 50, "bad{}"))).isNotEmpty();
        }
    }
}
