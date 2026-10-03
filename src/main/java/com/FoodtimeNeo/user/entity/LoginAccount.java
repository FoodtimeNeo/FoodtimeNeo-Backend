package com.FoodtimeNeo.user.entity;

import java.time.Instant;
import java.util.UUID;

/** Credential-bearing database projection: never serialize or store in a session. */
public record LoginAccount(UUID id, String email, String passwordHash, String displayName,
                           String role, String status, Instant passwordChangedAt) {
    public UserProfile profile() { return new UserProfile(id, email, displayName, role, status, passwordChangedAt); }

    @Override
    public String toString() { return "LoginAccount[id=" + id + ", credentials=<redacted>]"; }
}
