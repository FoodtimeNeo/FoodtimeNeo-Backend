package com.FoodtimeNeo.auth.security;

import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/** Redis stores identity, password version, mode and absolute expiry; never the password or its hash. */
public record SessionPrincipal(UUID userId, Instant passwordChangedAt, Instant expiresAt, SessionMode mode)
        implements Principal, Serializable {
    public SessionPrincipal(UUID userId, Instant passwordChangedAt, Instant expiresAt) {
        this(userId, passwordChangedAt, expiresAt, SessionMode.NORMAL);
    }
    @Override
    public String getName() { return userId.toString(); }
}
