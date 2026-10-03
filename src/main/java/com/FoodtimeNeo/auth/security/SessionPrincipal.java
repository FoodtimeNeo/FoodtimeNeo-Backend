package com.FoodtimeNeo.auth.security;

import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/** Redis stores identity, password version and absolute expiry; never the password or its hash. */
public record SessionPrincipal(UUID userId, Instant passwordChangedAt, Instant expiresAt)
        implements Principal, Serializable {
    @Override
    public String getName() { return userId.toString(); }
}
