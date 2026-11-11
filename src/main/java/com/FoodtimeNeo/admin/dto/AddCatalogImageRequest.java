package com.FoodtimeNeo.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCatalogImageRequest(@NotBlank @Size(max = 500) String imageUrl) { }
