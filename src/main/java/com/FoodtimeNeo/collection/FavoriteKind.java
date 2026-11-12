package com.FoodtimeNeo.collection;

import java.util.Locale;
import java.util.Optional;

public enum FavoriteKind {
    DISH("dish"),
    POST("post");

    private final String wireName;

    FavoriteKind(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static Optional<FavoriteKind> fromWire(String value) {
        if (value == null) return Optional.empty();
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "dish" -> Optional.of(DISH);
            case "post" -> Optional.of(POST);
            default -> Optional.empty();
        };
    }
}
