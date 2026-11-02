package com.FoodtimeNeo.dining.entity;

import java.util.UUID;

/** Joined read projection: a null stall ID represents an active hall with no active stalls. */
public record Stall(
    UUID diningHallId,
    UUID id,
    String name,
    String description,
    String coverImageUrl,
    String status,
    Integer sortOrder,
    java.math.BigDecimal averageScore,
    long reviewCount) {
  public Stall(
      UUID diningHallId,
      UUID id,
      String name,
      String description,
      String coverImageUrl,
      String status,
      Integer sortOrder) {
    this(diningHallId, id, name, description, coverImageUrl, status, sortOrder, null, 0);
  }
}
