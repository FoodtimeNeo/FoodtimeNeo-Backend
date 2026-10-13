package com.FoodtimeNeo.dish.entity;

import java.math.BigDecimal;
import java.util.UUID;

public record DishDetail(UUID id, UUID stallId, String stallName, UUID diningHallId,
                         String diningHallName, String name, String description, BigDecimal price) { }
