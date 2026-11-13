package com.FoodtimeNeo.authz;

import java.util.Map;
import java.util.UUID;

/**
 * Resource scope passed to the policy. Attributes are intentionally generic so a future
 * database-backed policy can reason about managed halls or other scopes without changing callers.
 */
public record AuthorizationContext(String resourceType, UUID resourceId, Map<String, Object> attributes) {
    public AuthorizationContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static AuthorizationContext none() {
        return new AuthorizationContext(null, null, Map.of());
    }

    public static AuthorizationContext resource(String type, UUID id) {
        return new AuthorizationContext(type, id, Map.of());
    }
}
