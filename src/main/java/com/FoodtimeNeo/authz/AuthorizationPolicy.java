package com.FoodtimeNeo.authz;

import com.FoodtimeNeo.user.entity.UserProfile;
import java.util.Set;

/**
 * Replaceable authorization policy. Business code depends on this interface, never on a
 * role-name comparison or a fixed superadmin boolean.
 */
public interface AuthorizationPolicy {
    boolean allows(UserProfile actor, Capability capability, AuthorizationContext context);

    Set<Capability> capabilities(UserProfile actor);

    /** Changes whenever the effective policy changes; clients use it to refresh cached capabilities. */
    String revision();
}
