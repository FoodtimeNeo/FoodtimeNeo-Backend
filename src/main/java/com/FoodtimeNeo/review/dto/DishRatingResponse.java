package com.FoodtimeNeo.review.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Transactionally maintained score summary; no scan of review details. */
public record DishRatingResponse(UUID dishId, BigDecimal averageScore, long reviewCount) { }
