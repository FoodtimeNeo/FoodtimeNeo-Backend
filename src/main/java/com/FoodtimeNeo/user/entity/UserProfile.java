package com.FoodtimeNeo.user.entity;

import java.time.Instant;
import java.util.UUID;

/** Current account identity; request projections may resolve role through ParticipationPolicy. */
public record UserProfile(UUID id, String email, String displayName, String role,
                          String status, Instant passwordChangedAt) { }
