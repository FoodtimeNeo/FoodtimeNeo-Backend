package com.FoodtimeNeo.auth.security;

import java.util.Locale;

public enum SessionMode {
    NORMAL("normal"),
    APPEAL_ONLY("appeal_only");

    private final String wireName;

    SessionMode(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static SessionMode fromWire(String value) {
        if (value == null) return NORMAL;
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "appeal_only" -> APPEAL_ONLY;
            default -> NORMAL;
        };
    }
}
