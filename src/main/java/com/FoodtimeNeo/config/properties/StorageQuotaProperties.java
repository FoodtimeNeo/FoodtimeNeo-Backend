package com.FoodtimeNeo.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.storage.quota")
public record StorageQuotaProperties(@Min(1) long userBytes, @Min(1) long totalBytes,
        @Min(0) long minFreeBytes, @Min(1) int concurrentUploads) { }
