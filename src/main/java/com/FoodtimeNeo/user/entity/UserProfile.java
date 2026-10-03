package com.FoodtimeNeo.user.entity;

import java.time.Instant;
import java.util.UUID;

/** Current database state used to recheck account status and password changes. */
public record UserProfile(UUID id, String email, String displayName, String role,
                          String status, Instant passwordChangedAt) { }
