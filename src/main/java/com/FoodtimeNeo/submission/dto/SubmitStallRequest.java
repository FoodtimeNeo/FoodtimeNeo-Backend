package com.FoodtimeNeo.submission.dto;
import com.FoodtimeNeo.common.api.MediaReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitStallRequest(@NotBlank @Size(max = 100) String name,
        @Size(max = 500, message = "简介不能超过500字") String description,
        @NotBlank @Pattern(regexp = MediaReference.PATTERN, message = MediaReference.MESSAGE) String coverImageUrl) { }
