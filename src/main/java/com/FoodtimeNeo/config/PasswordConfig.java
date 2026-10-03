package com.FoodtimeNeo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2id: 16-byte salt, 32-byte hash, 19 MiB memory, 2 iterations, parallelism 1.
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
        // Store the algorithm id to allow a future password policy upgrade.
        return new DelegatingPasswordEncoder("argon2id", Map.of("argon2id", argon2));
    }
}
