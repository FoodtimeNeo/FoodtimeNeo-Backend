package com.FoodtimeNeo.authz;

import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Current default mapping. Normal administrators keep catalog editing and moderation;
 * correction, report and appeal actions remain superadmin-only until a policy replacement says otherwise.
 */
@Component
public class DefaultAuthorizationPolicy implements AuthorizationPolicy {
    private static final Set<Capability> SUPERADMIN = Collections.unmodifiableSet(
            EnumSet.allOf(Capability.class));
    private static final Set<Capability> ADMIN = Collections.unmodifiableSet(
            EnumSet.of(Capability.MODERATION_REVIEW, Capability.CATALOG_EDIT,
                    Capability.CORRECTION_SUBMIT));
    private static final Set<Capability> USER = Collections.unmodifiableSet(
            EnumSet.of(Capability.CORRECTION_SUBMIT));
    private static final Set<Capability> NONE = Set.of();
    private static final String REVISION = "default-v2";

    @Override
    public boolean allows(UserProfile actor, Capability capability, AuthorizationContext context) {
        return capabilities(actor).contains(capability);
    }

    @Override
    public Set<Capability> capabilities(UserProfile actor) {
        if (actor == null || !"active".equals(actor.status())) return NONE;
        return switch (actor.role()) {
            case "superadmin" -> SUPERADMIN;
            case "admin" -> ADMIN;
            case "user" -> USER;
            default -> NONE;
        };
    }

    @Override
    public String revision() {
        return REVISION;
    }
}
