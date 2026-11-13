package com.FoodtimeNeo.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Deployment policy only; mailbox verification and stored roles remain unchanged. */
@ConfigurationProperties(prefix = "app.participation")
public record ParticipationProperties(boolean schoolEmailRequired) { }
