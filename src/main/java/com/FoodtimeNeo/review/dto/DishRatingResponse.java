package com.FoodtimeNeo.review.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Snapshot of the materialized view, not an on-demand aggregation. */
public record DishRatingResponse(UUID dishId, BigDecimal averageScore, long reviewCount) { }
