package com.FoodtimeNeo.dish.entity;

import java.math.BigDecimal;
import java.util.UUID;

/** A null dish ID represents a visible stall without active dishes. */
public record Dish(UUID stallId, UUID id, String name, BigDecimal price, BigDecimal averageScore,
                   long reviewCount, String priceUnit, String description, String coverImageUrl) {
    @org.apache.ibatis.annotations.AutomapConstructor
    public Dish { }
    public Dish(UUID stallId, UUID id, String name, BigDecimal price, BigDecimal averageScore, long reviewCount) {
        this(stallId, id, name, price, averageScore, reviewCount, "unspecified", "", null);
    }
}
