package com.FoodtimeNeo.recommendation;

import com.FoodtimeNeo.config.properties.DailyRecommendationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DailyRecommendationCacheTest {
    @Test void malformedCacheIsAMissAndWritesUseGlobalDateKeyWithTwoDayTtl() {
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        var config = DailyRecommendationProperties.defaults();
        var cache = new DailyRecommendationCacheService(redis, config);
        var date = LocalDate.of(2026, 11, 3);
        when(operations.get(cache.key(date))).thenReturn("corrupt");
        assertThat(cache.getBatchId(date)).isNull();
        var batch = UUID.randomUUID();
        var dish = UUID.randomUUID();
        cache.put(date, batch, List.of(dish));
        verify(operations).set("foodtime:recommendations:daily:v2:20261103", batch + "|" + dish,
                172800L, TimeUnit.SECONDS);
        when(operations.get(cache.key(date))).thenReturn(batch + "|" + dish);
        assertThat(cache.getBatchId(date)).isEqualTo(batch);
        assertThat(cache.key(date.plusDays(1))).isNotEqualTo(cache.key(date));
    }

    @Test void schedulerFailureCanBeRetried() {
        var service = mock(DailyRecommendationService.class);
        var date = LocalDate.of(2026, 11, 3);
        when(service.currentDate()).thenReturn(date);
        when(service.generate(date)).thenThrow(new IllegalStateException("simulated task failure")).thenReturn(null);
        var scheduler = new DailyRecommendationScheduler(service);
        scheduler.generate();
        scheduler.generate();
        verify(service, times(2)).generate(date);
    }
}
