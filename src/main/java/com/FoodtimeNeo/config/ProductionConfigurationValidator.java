package com.FoodtimeNeo.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Reject empty production connection settings before serving traffic. */
@Component
@Profile("prod")
public class ProductionConfigurationValidator {
    public ProductionConfigurationValidator(Environment environment) {
        for (String property : new String[] {
                "spring.datasource.url", "spring.datasource.username", "spring.datasource.password",
                "spring.data.redis.host", "spring.data.redis.password"
        }) {
            String value = environment.getRequiredProperty(property);
            if (value.isBlank() || value.contains("${")) {
                throw new IllegalStateException("Missing required production configuration: " + property);
            }
        }
    }
}
