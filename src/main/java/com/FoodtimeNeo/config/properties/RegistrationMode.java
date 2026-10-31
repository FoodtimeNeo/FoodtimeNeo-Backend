package com.FoodtimeNeo.config.properties;

/**
 * Registration policy for the whole deployment.
 * <p>
 * {@code OPEN} accepts any request that passes email verification, {@code INVITATION} additionally
 * requires a valid invitation code, and {@code CLOSED} rejects registration and code sending so the
 * mail sender is not drained while the door is shut.
 */
public enum RegistrationMode {
    OPEN, INVITATION, CLOSED
}
