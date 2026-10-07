package com.FoodtimeNeo.dining.entity;

import java.math.BigDecimal;
import java.util.UUID;

/** Database projection; display status and ordering remain server-controlled. */
public record DiningHall(UUID id, String name, String coverImageUrl, String description,
                         BigDecimal latitude, BigDecimal longitude, String status, int sortOrder) { }
