package com.FoodtimeNeo.media.dto;
import jakarta.validation.constraints.NotBlank;
public record ImageSubmissionRequest(@NotBlank String imageUrl) { }
