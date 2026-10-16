package com.FoodtimeNeo.user.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

@Component
public class DefaultDisplayNameGenerator {
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        return String.format(Locale.ROOT, "干饭人%06d", random.nextInt(1_000_000));
    }
}
