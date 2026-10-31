package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * A header that identifies the official clients. It is a speed bump, not proof of identity: a web
 * bundle is public, so any rule the browser can satisfy a script can copy. It exists to make casual
 * scripted registrations fail and to give operations a switch during an attack.
 */
@Validated
@ConfigurationProperties(prefix = "app.auth.client-header")
public record ClientHeaderProperties(
        @NotNull ClientHeaderMode mode,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,64}") String headerName,
        @NotEmpty List<@NotBlank String> allowedClients,
        @NotEmpty List<@NotBlank String> paths) { }
