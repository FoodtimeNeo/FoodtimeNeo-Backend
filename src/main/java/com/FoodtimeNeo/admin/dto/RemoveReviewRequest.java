package com.FoodtimeNeo.admin.dto;
import jakarta.validation.constraints.NotBlank;
public record RemoveReviewRequest(@NotBlank String reason) { }
