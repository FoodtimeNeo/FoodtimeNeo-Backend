package com.FoodtimeNeo.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "全站统一每日推荐")
public record DailyRecommendationResponse(LocalDate date, String algorithmVersion, int total,
                                          List<RecommendedDishResponse> items) { }
