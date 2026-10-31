package com.FoodtimeNeo.submission.dto;
import com.FoodtimeNeo.common.api.MediaReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitDishRequest(@NotBlank @Size(max = 120) String name,
        @Size(max = 500, message = "简介不能超过500字") String description,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotBlank @Pattern(regexp = MediaReference.PATTERN, message = MediaReference.MESSAGE) String imageUrl) { }
