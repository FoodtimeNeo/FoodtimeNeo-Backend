package com.FoodtimeNeo.admin.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogEditRequest(UUID parentId, @NotBlank @Size(max = 120) String name,
        @Size(max = 500, message = "简介不能超过500字") String description,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal price, String coverImageUrl,
        @Pattern(regexp = "active|inactive") String status, Integer sortOrder) { }
