package com.FoodtimeNeo.auth;

import com.FoodtimeNeo.config.PasswordConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordStorageTest {
    private final PasswordEncoder encoder = new PasswordConfig().passwordEncoder();

    @Test
    void hashesHaveIndependentSaltsAndOnlyMatchTheCorrectPassword() {
        String first = encoder.encode("Foodtime2026!");
        String second = encoder.encode("Foodtime2026!");
        assertThat(first).startsWith("{argon2id}$argon2id$v=19$m=19456,t=2,p=1$").isNotEqualTo(second);
        assertThat(encoder.matches("Foodtime2026!", first)).isTrue();
        assertThat(encoder.matches("incorrect2026", first)).isFalse();
    }

    @Test
    void passwordSuffixesBeyond72BytesStillAffectAuthentication() {
        String password = "A1" + "x".repeat(100) + "z";
        String hash = encoder.encode(password);
        assertThat(encoder.matches(password, hash)).isTrue();
        assertThat(encoder.matches("A1" + "x".repeat(100) + "y", hash)).isFalse();
    }
}
