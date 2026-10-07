package com.FoodtimeNeo.dining.entity;

import java.util.UUID;

/** Joined read projection: a null stall ID represents an active hall with no active stalls. */
public record Stall(UUID diningHallId, UUID id, String name, String description,
                    String coverImageUrl, String floor, String status, Integer sortOrder) { }
