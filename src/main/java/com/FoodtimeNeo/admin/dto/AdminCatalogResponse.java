package com.FoodtimeNeo.admin.dto;
import java.math.BigDecimal;
import java.util.UUID;

/** parentId aliases the real foreign key. Dish cover is derived from approved images, never a dish column. */
public record AdminCatalogResponse(UUID id, UUID parentId, String name, String description,
        BigDecimal price, String coverImageUrl, String status, Integer sortOrder) { }
