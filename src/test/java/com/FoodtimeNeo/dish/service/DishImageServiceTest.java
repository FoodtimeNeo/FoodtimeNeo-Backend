package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.mapper.DishImageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DishImageServiceTest {
    @Test
    void missingVisibleDishReturns404() {
        var mapper = mock(DishImageMapper.class);
        assertThatThrownBy(() -> new DishImageService(mapper).list(UUID.randomUUID(), new PageRequest(1, 20)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatus().value()).isEqualTo(404));
    }
    @Test
    void databaseFailureReturns503WithoutInternalDetails() {
        var mapper = mock(DishImageMapper.class);
        UUID id = UUID.randomUUID();
        when(mapper.findVisiblePage(id, 20, 0)).thenThrow(new DataAccessResourceFailureException("private-sql"));
        assertThatThrownBy(() -> new DishImageService(mapper).list(id, new PageRequest(1, 20)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("DISH_IMAGES_UNAVAILABLE");
                    assertThat(e.getStatus().value()).isEqualTo(503);
                }).hasMessageNotContaining("private-sql");
    }
}
