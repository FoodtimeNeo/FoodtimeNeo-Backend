package com.FoodtimeNeo.dining.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dining.entity.Stall;
import com.FoodtimeNeo.dining.mapper.StallMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StallServiceTest {
    private final StallMapper mapper = mock(StallMapper.class);
    private final StallService service = new StallService(mapper);
    private final UUID hall = UUID.randomUUID();
    @Test
    void preservesOrderingAndProjectsOnlyDisplayFields() {
        var one = new Stall(hall, UUID.randomUUID(), "第一档口", "简介", "https://example.test/stall.jpg", "一层", "active", 1);
        var two = new Stall(hall, UUID.randomUUID(), "第二档口", null, null, null, "active", 2);
        when(mapper.findActiveByDiningHall(hall)).thenReturn(List.of(one, two));
        var result = service.listByDiningHall(hall);
        assertThat(result).extracting(row -> row.id()).containsExactly(one.id(), two.id());
        assertThat(result.get(0).diningHallId()).isEqualTo(hall);
        assertThat(result.get(0).floor()).isEqualTo("一层");
        assertThat(result.get(1).description()).isNull();
    }
    @Test
    void activeHallWithoutActiveStallsReturnsEmptyArray() {
        when(mapper.findActiveByDiningHall(hall)).thenReturn(List.of(new Stall(hall, null, null, null, null, null, null, null)));
        assertThat(service.listByDiningHall(hall)).isEmpty();
    }
    @Test
    void absentOrInactiveHallReturns404() {
        when(mapper.findActiveByDiningHall(hall)).thenReturn(List.of());
        assertThatThrownBy(() -> service.listByDiningHall(hall)).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getCode()).isEqualTo("DINING_HALL_NOT_FOUND");
            assertThat(error.getStatus().value()).isEqualTo(404);
        });
    }
    @Test
    void databaseFailureDoesNotExposeSqlOrConnectionDetails() {
        when(mapper.findActiveByDiningHall(hall)).thenThrow(new DataAccessResourceFailureException("private-sql-host"));
        assertThatThrownBy(() -> service.listByDiningHall(hall)).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getCode()).isEqualTo("STALLS_UNAVAILABLE");
            assertThat(error.getStatus().value()).isEqualTo(503);
        }).hasMessageNotContaining("private-sql-host");
    }
}
