package com.FoodtimeNeo.recommendation;

import com.FoodtimeNeo.config.properties.DailyRecommendationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DailyRecommendationCacheService {
    private static final Logger LOG = LoggerFactory.getLogger(DailyRecommendationCacheService.class);
    private final StringRedisTemplate redis;
    private final DailyRecommendationProperties properties;

    public DailyRecommendationCacheService(StringRedisTemplate redis, DailyRecommendationProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public UUID getBatchId(LocalDate date) {
        try {
            String value = redis.opsForValue().get(key(date));
            if (value == null || value.isBlank()) return null;
            int separator = value.indexOf('|');
            return UUID.fromString(separator < 0 ? value : value.substring(0, separator));
        } catch (RuntimeException exception) {
            LOG.warn("Daily recommendation cache read failed: {}", exception.getClass().getSimpleName());
            return null;
        }
    }

    public void put(LocalDate date, UUID batchId, List<UUID> dishIds) {
        try {
            String ids = dishIds.stream().map(UUID::toString).reduce((a, b) -> a + "," + b).orElse("");
            redis.opsForValue().set(key(date), batchId + "|" + ids,
                    properties.cacheTtl().toSeconds(), TimeUnit.SECONDS);
        } catch (RuntimeException exception) {
            // PostgreSQL remains the source of truth; cache failure must not fail generation.
            LOG.warn("Daily recommendation cache write failed: {}", exception.getClass().getSimpleName());
        }
    }

    public String key(LocalDate date) { return "foodtime:recommendations:daily:v2:" + date.toString().replace("-", ""); }
}
