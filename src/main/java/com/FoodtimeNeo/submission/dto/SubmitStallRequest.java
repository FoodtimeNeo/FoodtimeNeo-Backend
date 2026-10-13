package com.FoodtimeNeo.submission.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitStallRequest(@NotBlank @Size(max = 100) String name, String description,
        @NotBlank String coverImageUrl) { }
