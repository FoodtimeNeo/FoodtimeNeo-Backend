package com.FoodtimeNeo.notification;

import java.util.Locale;
import java.util.Optional;

public enum NotificationCategory {
    MESSAGE("message", true),
    INTERACTION("interaction", false),
    NOTICE("notice", false),
    FEEDBACK("feedback", false),
    CORRECTION("correction", false),
    REPORT("report", false),
    APPEAL("appeal", false);

    private final String wireName;
    private final boolean defaultEnabled;

    NotificationCategory(String wireName, boolean defaultEnabled) {
        this.wireName = wireName;
        this.defaultEnabled = defaultEnabled;
    }

    public String wireName() {
        return wireName;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public static Optional<NotificationCategory> fromWire(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        for (NotificationCategory category : values()) {
            if (category.wireName.equals(normalized)) return Optional.of(category);
        }
        return Optional.empty();
    }
}
