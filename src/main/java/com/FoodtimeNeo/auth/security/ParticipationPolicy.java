package com.FoodtimeNeo.auth.security;

import com.FoodtimeNeo.config.properties.ParticipationProperties;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.stereotype.Component;

/** Resolves effective ordinary-member access without rewriting stored mailbox identity or roles. */
@Component
public final class ParticipationPolicy {
    private final ParticipationProperties properties;

    public ParticipationPolicy(ParticipationProperties properties) {
        this.properties = properties;
    }

    public String effectiveRole(String storedRole) {
        return !properties.schoolEmailRequired() && "visitor".equals(storedRole) ? "user" : storedRole;
    }

    public UserProfile resolve(UserProfile stored) {
        if (stored == null || !"active".equals(stored.status())) return stored;
        String role = effectiveRole(stored.role());
        if (role.equals(stored.role())) return stored;
        return new UserProfile(stored.id(), stored.email(), stored.displayName(), role,
                stored.status(), stored.passwordChangedAt());
    }
}
