package com.FoodtimeNeo.search;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Location is transient request data. Never include this request in a persistent catalog cache. */
public record SearchRequest(String q, List<UUID> diningHallIds, BigDecimal minPrice,
        BigDecimal maxPrice, BigDecimal minRating, String sort, String type,
        Double latitude, Double longitude, UUID stallId, Integer page, Integer size,
        Integer previewSize) { }
