package com.FoodtimeNeo.dashboard;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {
    private final DashboardStore store = mock(DashboardStore.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final DashboardService service = new DashboardService(store, redis, "spring:session");

    private static UserProfile user(String role) {
        return new UserProfile(UUID.randomUUID(), "a@b.c", "t", role, "active", Instant.now());
    }

    @Test
    void nonSuperadminIsForbidden() {
        assertThatThrownBy(() -> service.overview(user("admin"), 7))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo("ACCESS_DENIED"));
        assertThatThrownBy(() -> service.overview(user("user"), 7))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo("ACCESS_DENIED"));
    }

    @Test
    void daysOutOfRangeIsRejected() {
        assertThatThrownBy(() -> service.overview(user("superadmin"), 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo("VALIDATION_ERROR"));
        assertThatThrownBy(() -> service.overview(user("superadmin"), 32))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo("VALIDATION_ERROR"));
    }

    @Test
    void assemblesTotalsTrendAndOnlineCount() {
        when(store.totals()).thenReturn(new DashboardStore.Totals(10, 20, 30, 40, 1, 2, 3));
        when(store.dailyNewCounts(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(store.dailyActiveUsers(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(redis.execute(any(RedisCallback.class))).thenReturn(4L);
        DashboardDtos.DashboardResponse response = service.overview(user("superadmin"), 7);
        assertThat(response.registeredUsers()).isEqualTo(10);
        assertThat(response.onlineUsers()).isEqualTo(4);
        assertThat(response.trend()).hasSize(7);
        assertThat(response.trend().get(0).newUsers()).isZero();
        assertThat(response.trend()).extracting(DashboardDtos.TrendDay::day).doesNotHaveDuplicates();
    }

    @Test
    void onlineUsersCountsSessionKeysAndSkipsExpiryMarkers() {
        when(store.totals()).thenReturn(new DashboardStore.Totals(0, 0, 0, 0, 0, 0, 0));
        when(store.dailyNewCounts(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(store.dailyActiveUsers(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        RedisConnection connection =
                mock(RedisConnection.class);
        Cursor<byte[]> cursor =
                mock(org.springframework.data.redis.core.Cursor.class);
        when(connection.scan(any(ScanOptions.class))).thenReturn(cursor);
        byte[][] keys = {
                "spring:session:sessions:aaa".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "spring:session:sessions:bbb".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "spring:session:sessions:expires:111".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "foodtime:auth:email:{verification}:x".getBytes(java.nio.charset.StandardCharsets.UTF_8),
        };
        java.util.concurrent.atomic.AtomicInteger index = new java.util.concurrent.atomic.AtomicInteger();
        when(cursor.hasNext()).thenAnswer(i -> index.get() < keys.length);
        when(cursor.next()).thenAnswer(i -> keys[index.getAndIncrement()]);
        when(redis.execute(any(RedisCallback.class))).thenAnswer(i -> {
            @SuppressWarnings("unchecked")
            RedisCallback<Long> cb = i.getArgument(0);
            return cb.doInRedis(connection);
        });
        assertThat(service.overview(user("superadmin"), 1).onlineUsers()).isEqualTo(2);
    }

    @Test
    void emptyTrendDaysAreZeroFilledInOrder() {
        when(store.totals()).thenReturn(new DashboardStore.Totals(0, 0, 0, 0, 0, 0, 0));
        when(store.dailyNewCounts(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(store.dailyActiveUsers(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(redis.execute(any(RedisCallback.class))).thenReturn(null);
        DashboardDtos.DashboardResponse response = service.overview(user("superadmin"), 3);
        assertThat(response.trend()).hasSize(3);
        List<String> days = response.trend().stream().map(DashboardDtos.TrendDay::day).toList();
        assertThat(days).isSorted();
        assertThat(response.onlineUsers()).isZero();
    }
}
