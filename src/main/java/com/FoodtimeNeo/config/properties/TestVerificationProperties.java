package com.FoodtimeNeo.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Development-only simulated verification; never proof of mailbox ownership. */
@ConfigurationProperties(prefix = "app.test-verification")
public record TestVerificationProperties(boolean enabled) {
    public boolean accepts(String code) {
        return enabled && "000000".equals(code);
    }
}
