package com.FoodtimeNeo.auth.dto;
import java.time.Instant;
public record PasswordResetGrantResponse(String resetToken, Instant expiresAt) {
    @Override public String toString() { return "PasswordResetGrantResponse[token=<redacted>, expiresAt="+expiresAt+"]"; }
}
