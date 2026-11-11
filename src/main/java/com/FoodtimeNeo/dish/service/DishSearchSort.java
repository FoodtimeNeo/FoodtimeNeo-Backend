package com.FoodtimeNeo.dish.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Whitelisted sort names; SQL fragments stay in the mapper and are never built from client input. */
public enum DishSearchSort {
    RELEVANCE("relevance"),
    PRICE_ASC("price_asc"),
    PRICE_DESC("price_desc"),
    RATING_DESC("rating_desc");

    private final String wireName;

    DishSearchSort(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static Optional<DishSearchSort> fromWire(String value) {
        if (value == null || value.isBlank()) return Optional.of(RELEVANCE);
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(sort -> sort.wireName.equals(normalized))
                .findFirst();
    }
}
