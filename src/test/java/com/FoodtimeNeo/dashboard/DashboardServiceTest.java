package com.FoodtimeNeo.dashboard;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;

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
    private final OnlinePresence presence = mock(OnlinePresence.class);
    private final DashboardService service = new DashboardService(store, presence);

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
        when(store.totals()).thenReturn(new DashboardStore.Totals(12, 10, 20, 30, 40, 1, 2, 3));
        when(store.totalUsersBefore(any(Instant.class))).thenReturn(12L);
        when(store.dailyNewCounts(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(store.dailyActiveUsers(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(presence.count()).thenReturn(4L);
        DashboardDtos.DashboardResponse response = service.overview(user("superadmin"), 7);
        assertThat(response.registeredUsers()).isEqualTo(10);
        assertThat(response.totalUsers()).isEqualTo(12);
        assertThat(response.trend()).extracting(DashboardDtos.TrendDay::totalUsers).containsOnly(12L);
        assertThat(response.onlineUsers()).isEqualTo(4);
        assertThat(response.trend()).hasSize(7);
        assertThat(response.trend().get(0).newUsers()).isZero();
        assertThat(response.trend()).extracting(DashboardDtos.TrendDay::day).doesNotHaveDuplicates();
    }

    @Test
    void onlineUsersComesFromPresenceCount() {
        when(store.totals()).thenReturn(new DashboardStore.Totals(0, 0, 0, 0, 0, 0, 0, 0));
        when(store.dailyNewCounts(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(store.dailyActiveUsers(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(presence.count()).thenReturn(2L);
        assertThat(service.overview(user("superadmin"), 1).onlineUsers()).isEqualTo(2);
    }

    @Test
    void emptyTrendDaysAreZeroFilledInOrder() {
        when(store.totals()).thenReturn(new DashboardStore.Totals(0, 0, 0, 0, 0, 0, 0, 0));
        when(store.dailyNewCounts(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(store.dailyActiveUsers(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        when(presence.count()).thenReturn(0L);
        DashboardDtos.DashboardResponse response = service.overview(user("superadmin"), 3);
        assertThat(response.trend()).hasSize(3);
        List<String> days = response.trend().stream().map(DashboardDtos.TrendDay::day).toList();
        assertThat(days).isSorted();
        assertThat(response.onlineUsers()).isZero();
    }

    @Test
    void thirtyDaysKeepHistoricalTotalsAndZeroFillGaps() {
        var today = java.time.LocalDate.now(DashboardService.BUSINESS_ZONE);
        when(store.totals()).thenReturn(new DashboardStore.Totals(105, 100, 0, 0, 0, 0, 0, 0));
        when(store.totalUsersBefore(any(Instant.class))).thenReturn(100L);
        when(store.dailyNewCounts(any(Instant.class), any(Instant.class))).thenReturn(Map.of(
                today.minusDays(29).toString(), Map.of("newUsers", 2L),
                today.toString(), Map.of("newUsers", 3L)));
        when(store.dailyActiveUsers(any(Instant.class), any(Instant.class))).thenReturn(Map.of());
        var response = service.overview(user("superadmin"), 30);
        assertThat(response.trend()).hasSize(30);
        assertThat(response.trend().getFirst().totalUsers()).isEqualTo(102);
        assertThat(response.trend().get(28).totalUsers()).isEqualTo(102);
        assertThat(response.trend().getLast().totalUsers()).isEqualTo(105);
        assertThat(response.trend().getLast().newUsers()).isEqualTo(3);
    }
}
