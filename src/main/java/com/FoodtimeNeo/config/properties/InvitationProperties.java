package com.FoodtimeNeo.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.invitation")
public record InvitationProperties(String secret) {
    @Override
    public String toString() { return "InvitationProperties[credentials=<redacted>]"; }
}
