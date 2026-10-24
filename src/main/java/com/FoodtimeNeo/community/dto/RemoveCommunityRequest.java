package com.FoodtimeNeo.community.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record RemoveCommunityRequest(@NotBlank @Size(max=500) String reason) { }
