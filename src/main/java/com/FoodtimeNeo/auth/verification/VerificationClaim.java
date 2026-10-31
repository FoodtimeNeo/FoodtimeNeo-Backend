package com.FoodtimeNeo.auth.verification;

/** A verification lease obtained from a valid, email-bound Redis proof. */
public record VerificationClaim(String email, String token) {

    @Override
    public String toString() { return "VerificationClaim[<redacted>]"; }
}
