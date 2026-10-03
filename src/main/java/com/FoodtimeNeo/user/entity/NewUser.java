package com.FoodtimeNeo.user.entity;

import java.util.UUID;
import java.time.Instant;

/** Persistence input only; registration responses use a separate DTO. */
public record NewUser(UUID id, String email, String passwordHash, String displayName, Instant emailVerifiedAt) {
    @Override
    public String toString() {
        return "NewUser[id=" + id + ", credentials=<redacted>]";
    }
}
