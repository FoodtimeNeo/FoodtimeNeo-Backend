package com.FoodtimeNeo.admin.dto;

import java.time.Instant;
import java.util.UUID;

/** Fields from users; reviewCount is a join aggregate, not a stored user column. */
public record AdminUserResponse(UUID id, String email, String displayName, String avatarUrl,
                                String role, String status, Instant createdAt, long reviewCount) { }
