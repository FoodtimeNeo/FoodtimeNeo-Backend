package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.entity.Dish;
import com.FoodtimeNeo.dish.mapper.DishMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DishServiceTest {
    private final DishMapper mapper = mock(DishMapper.class);
    private final DishService service = new DishService(mapper);
    private final UUID stall = UUID.randomUUID();

    @Test
    void preservesOrderAndExactPricesIncludingZeroAndDatabaseMaximum() {
        var one = new Dish(stall, UUID.randomUUID(), "免费汤", new BigDecimal("0.00"));
        var two = new Dish(stall, UUID.randomUUID(), "测试菜品", new BigDecimal("99999999.99"));
        when(mapper.findActiveByStall(stall)).thenReturn(List.of(one, two));
        var result = service.listByStall(stall);
        assertThat(result).extracting(row -> row.id()).containsExactly(one.id(), two.id());
        assertThat(result.get(0).stallId()).isEqualTo(stall);
        assertThat(result.get(0).price()).isEqualTo(new BigDecimal("0.00"));
        assertThat(result.get(1).price()).isEqualTo(new BigDecimal("99999999.99"));
    }

    @Test
    void visibleStallWithoutActiveDishesReturnsEmptyArray() {
        when(mapper.findActiveByStall(stall)).thenReturn(List.of(new Dish(stall, null, null, null)));
        assertThat(service.listByStall(stall)).isEmpty();
    }

    @Test
    void missingOrInvisibleStallReturns404() {
        when(mapper.findActiveByStall(stall)).thenReturn(List.of());
        assertThatThrownBy(() -> service.listByStall(stall)).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getCode()).isEqualTo("STALL_NOT_FOUND");
            assertThat(error.getStatus().value()).isEqualTo(404);
        });
    }

    @Test
    void databaseFailureDoesNotExposeConnectionDetails() {
        when(mapper.findActiveByStall(stall)).thenThrow(new DataAccessResourceFailureException("private-sql-host"));
        assertThatThrownBy(() -> service.listByStall(stall)).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getCode()).isEqualTo("DISHES_UNAVAILABLE");
            assertThat(error.getStatus().value()).isEqualTo(503);
        }).hasMessageNotContaining("private-sql-host");
    }
}
