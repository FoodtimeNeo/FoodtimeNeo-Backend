package com.FoodtimeNeo.api.dish;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.dish.service.DishDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DishDetailControllerTest {
    @Test
    void malformedUuidFailsBeforeQuerying() throws Exception {
        var service = mock(DishDetailService.class);
        MockMvcBuilders.standaloneSetup(new DishDetailController(service)).setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/api/v1/dishes/not-a-uuid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }
    @Test
    void preservesUnavailableStatusAndErrorCode() throws Exception {
        var service = mock(DishDetailService.class);
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new BusinessException("DISH_DETAIL_UNAVAILABLE", "菜品详情暂不可用", HttpStatus.SERVICE_UNAVAILABLE));
        MockMvcBuilders.standaloneSetup(new DishDetailController(service)).setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/api/v1/dishes/{id}", id))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("DISH_DETAIL_UNAVAILABLE"));
    }
}
