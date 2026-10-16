package com.FoodtimeNeo.dish.entity;

import java.time.Instant;
import java.util.UUID;

/** A null image ID is the visible parent sentinel, also retaining the total for an empty page. */
public record DishImagePageRow(UUID dishId, UUID id, String imageUrl, Instant createdAt, Long total) { }
