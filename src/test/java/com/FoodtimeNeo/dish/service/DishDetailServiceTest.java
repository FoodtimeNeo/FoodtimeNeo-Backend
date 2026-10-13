package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.mapper.DishDetailMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DishDetailServiceTest {
    private final DishDetailMapper mapper = mock(DishDetailMapper.class);
    private final DishDetailService service = new DishDetailService(mapper);

    @Test
    void missingVisibleDishReturnsDomain404() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getCode()).isEqualTo("DISH_NOT_FOUND");
            assertThat(error.getStatus().value()).isEqualTo(404);
        });
    }
    @Test
    void queryFailureReturns503WithoutDatabaseDetails() {
        UUID id = UUID.randomUUID();
        when(mapper.findVisible(id)).thenThrow(new DataAccessResourceFailureException("private-sql-host"));
        assertThatThrownBy(() -> service.get(id)).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getCode()).isEqualTo("DISH_DETAIL_UNAVAILABLE");
            assertThat(error.getStatus().value()).isEqualTo(503);
        }).hasMessageNotContaining("private-sql-host");
    }
}
