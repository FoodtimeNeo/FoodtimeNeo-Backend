package com.FoodtimeNeo.dish.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DishSearchResponse(UUID id, String name, BigDecimal price, UUID stallId,
                                 String stallName, UUID diningHallId, String diningHallName) {}
