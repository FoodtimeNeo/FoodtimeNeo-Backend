package com.FoodtimeNeo.dish.entity;

import java.math.BigDecimal;
import java.util.UUID;

/** A null dish ID represents a visible stall without active dishes. */
public record Dish(UUID stallId, UUID id, String name, BigDecimal price) { }
