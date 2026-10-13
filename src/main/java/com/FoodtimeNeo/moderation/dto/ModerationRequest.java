package com.FoodtimeNeo.moderation.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModerationRequest(@NotNull @Pattern(regexp = "approve|reject") String action,
        String reason, @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal price) { }
