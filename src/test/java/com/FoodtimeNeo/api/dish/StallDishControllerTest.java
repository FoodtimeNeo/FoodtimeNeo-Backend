package com.FoodtimeNeo.api.dish;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.common.web.RequestIdFilter;
import com.FoodtimeNeo.dish.dto.DishListItemResponse;
import com.FoodtimeNeo.dish.service.DishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StallDishControllerTest {
    private final DishService service = mock(DishService.class);
    private final UUID stall = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new StallDishController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).addFilters(new RequestIdFilter()).build();
    }

    @Test
    void usesPathStallAndReturnsNumericPriceAndOnlyListFields() throws Exception {
        UUID dish = UUID.randomUUID();
        when(service.listByStall(stall)).thenReturn(List.of(new DishListItemResponse(dish, stall, "红烧肉", new BigDecimal("12.30"))));
        mvc.perform(get("/api/v1/stalls/{id}/dishes", stall).header("X-Request-Id", "dish-list-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(dish.toString()))
                .andExpect(jsonPath("$.data[0].stallId").value(stall.toString()))
                .andExpect(jsonPath("$.data[0].name").value("红烧肉"))
                .andExpect(jsonPath("$.data[0].price").isNumber()).andExpect(jsonPath("$.data[0].price").value(12.3))
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].imageUrl").doesNotExist())
                .andExpect(header().string("X-Request-Id", "dish-list-test"));
        verify(service).listByStall(stall);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "123", "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz"})
    void malformedStallIdFailsBeforeCallingService(String id) throws Exception {
        mvc.perform(get("/api/v1/stalls/{id}/dishes", id))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void visibleStallWithoutDishesReturns200EmptyArray() throws Exception {
        when(service.listByStall(stall)).thenReturn(List.of());
        mvc.perform(get("/api/v1/stalls/{id}/dishes", stall))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray()).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void preservesNotFoundDomainError() throws Exception {
        when(service.listByStall(stall)).thenThrow(new BusinessException("STALL_NOT_FOUND", "档口不存在或暂不可用", HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/v1/stalls/{id}/dishes", stall))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("STALL_NOT_FOUND"));
    }

    @Test
    void preservesUnavailableDomainError() throws Exception {
        when(service.listByStall(stall)).thenThrow(new BusinessException("DISHES_UNAVAILABLE", "菜品列表服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE));
        mvc.perform(get("/api/v1/stalls/{id}/dishes", stall))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("DISHES_UNAVAILABLE"));
    }
}
