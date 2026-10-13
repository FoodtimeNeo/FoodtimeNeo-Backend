package com.FoodtimeNeo.submission.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitDishRequest(@NotBlank @Size(max = 120) String name, String description,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal price, @NotBlank String imageUrl) { }
