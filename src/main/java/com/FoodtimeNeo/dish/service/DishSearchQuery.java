package com.FoodtimeNeo.dish.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Normalized query spec shared by the page and count statements. q may be empty. */
public record DishSearchQuery(String q, List<UUID> diningHallIds, BigDecimal minPrice,
                              BigDecimal maxPrice, BigDecimal minRating, String sort) {
    public DishSearchQuery {
        diningHallIds = diningHallIds == null ? List.of() : List.copyOf(diningHallIds);
    }
}
