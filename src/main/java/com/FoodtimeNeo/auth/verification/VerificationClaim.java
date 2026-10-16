package com.FoodtimeNeo.auth.verification;

/** Real Redis verification lease, or an explicit test verification without a lease. */
public record VerificationClaim(String email, String token, boolean testVerification) {
    public VerificationClaim(String email, String token) { this(email, token, false); }

    @Override
    public String toString() { return "VerificationClaim[<redacted>]"; }
}
