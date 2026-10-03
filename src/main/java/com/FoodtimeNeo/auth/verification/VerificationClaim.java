package com.FoodtimeNeo.auth.verification;

/** Private lease used to coordinate Redis validation and the atomic PostgreSQL insert. */
public record VerificationClaim(String email, String token) {
    @Override
    public String toString() { return "VerificationClaim[<redacted>]"; }
}
