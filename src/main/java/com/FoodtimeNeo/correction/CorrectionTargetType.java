package com.FoodtimeNeo.correction;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum CorrectionTargetType {
    DINING_HALL("dining_hall", "dining-hall", "foodtime.dining_halls", 100,
            Set.of("name", "description")),
    STALL("stall", "stall", "foodtime.stalls", 100,
            Set.of("name", "description")),
    DISH("dish", "dish", "foodtime.dishes", 120,
            Set.of("name", "description", "price", "priceUnit"));

    private final String wireName;
    private final String mediaType;
    private final String table;
    private final int nameLimit;
    private final Set<String> fields;

    CorrectionTargetType(String wireName, String mediaType, String table, int nameLimit, Set<String> fields) {
        this.wireName = wireName;
        this.mediaType = mediaType;
        this.table = table;
        this.nameLimit = nameLimit;
        this.fields = fields;
    }

    public String wireName() {
        return wireName;
    }

    public String table() {
        return table;
    }

    public int nameLimit() {
        return nameLimit;
    }

    public Set<String> fields() {
        return fields;
    }

    public boolean supports(String field) {
        return fields.contains(field);
    }

    public static Optional<CorrectionTargetType> fromWire(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.strip().toLowerCase(Locale.ROOT).replace('-', '_');
        for (CorrectionTargetType type : values()) {
            if (type.wireName.equals(normalized) || type.mediaType.replace('-', '_').equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
