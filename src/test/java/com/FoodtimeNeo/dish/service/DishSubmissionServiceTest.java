package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.mapper.DishSubmissionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DishSubmissionServiceTest {
    @Test
    void databaseFailureReturns503WithoutInternalDetails() {
        var mapper = mock(DishSubmissionMapper.class);
        UUID id = UUID.randomUUID();
        when(mapper.findOwnedPage(id, 20, 0)).thenThrow(new DataAccessResourceFailureException("private-sql"));
        assertThatThrownBy(() -> new DishSubmissionService(mapper).list(id, new PageRequest(1, 20)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("DISH_SUBMISSIONS_UNAVAILABLE");
                    assertThat(e.getStatus().value()).isEqualTo(503);
                }).hasMessageNotContaining("private-sql");
    }
}
