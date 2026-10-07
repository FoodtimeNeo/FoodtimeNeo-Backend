package com.FoodtimeNeo.dining.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dining.entity.DiningHall;
import com.FoodtimeNeo.dining.mapper.DiningHallMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiningHallServiceTest {
    private final DiningHallMapper mapper = mock(DiningHallMapper.class);
    private final DiningHallService service = new DiningHallService(mapper);

    @Test
    void preservesDatabaseOrderingAndNullableFieldsInPublicProjection() {
        var one = new DiningHall(UUID.randomUUID(), "第一食堂", "https://example.test/hall.jpg", "食堂简介",
                new BigDecimal("39.9520000"), new BigDecimal("116.3500000"), "active", 0);
        var two = new DiningHall(UUID.randomUUID(), "第二食堂", null, null, null, null, "active", 1);
        when(mapper.findActive()).thenReturn(List.of(one, two));
        var result = service.list();
        assertThat(result).extracting(hall -> hall.id()).containsExactly(one.id(), two.id());
        assertThat(result.get(0).latitude()).isEqualTo(one.latitude());
        assertThat(result.get(0).coverImageUrl()).isEqualTo(one.coverImageUrl());
        assertThat(result.get(1).description()).isNull();
        assertThat(result.get(1).longitude()).isNull();
    }

    @Test
    void noActiveHallsReturnsEmptyList() {
        when(mapper.findActive()).thenReturn(List.of());
        assertThat(service.list()).isEmpty();
    }

    @Test
    void databaseFailureReturnsSafeUnavailableError() {
        when(mapper.findActive()).thenThrow(new DataAccessResourceFailureException("private-sql-host"));
        assertThatThrownBy(service::list).isInstanceOfSatisfying(BusinessException.class, e -> {
            assertThat(e.getCode()).isEqualTo("DINING_HALLS_UNAVAILABLE");
            assertThat(e.getStatus().value()).isEqualTo(503);
        }).hasMessageNotContaining("private-sql-host");
    }
}
