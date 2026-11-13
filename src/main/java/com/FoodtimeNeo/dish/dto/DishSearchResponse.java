package com.FoodtimeNeo.dish.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** averageScore is null when the dish has no formal review; reviewCount is then 0. */
public record DishSearchResponse(UUID id, String name, BigDecimal price, UUID stallId,
                                 String stallName, UUID diningHallId, String diningHallName,
                                 BigDecimal averageScore, long reviewCount) {}
