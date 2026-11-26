package com.FoodtimeNeo.admin.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogEditRequest(UUID parentId, @NotBlank @Size(max = 120) String name,
        @Size(max = 500, message = "简介不能超过500字") String description,
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal price, String coverImageUrl,
        @Pattern(regexp = "active|inactive") String status, Integer sortOrder, String priceUnit,
        @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 7) BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 7) BigDecimal longitude,
        Boolean clearCoordinates, Long expectedRevision) {
    public CatalogEditRequest(UUID parentId, String name, String description, BigDecimal price,
            String coverImageUrl, String status, Integer sortOrder, String priceUnit,
            BigDecimal latitude, BigDecimal longitude, Boolean clearCoordinates) {
        this(parentId, name, description, price, coverImageUrl, status, sortOrder, priceUnit,
                latitude, longitude, clearCoordinates, null);
    }
    public CatalogEditRequest(UUID parentId, String name, String description, BigDecimal price,
            String coverImageUrl, String status, Integer sortOrder, String priceUnit) {
        this(parentId, name, description, price, coverImageUrl, status, sortOrder, priceUnit,
                null, null, null, null);
    }
}
