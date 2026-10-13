package com.FoodtimeNeo.api.dish;

import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.dish.service.DishImageService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DishImageControllerTest {
    @ParameterizedTest
    @ValueSource(strings = {"page=0", "page=-1", "size=0", "size=101", "page=nope", "size=2.2", "page=2147483648"})
    void invalidPagingFailsBeforeQuerying(String query) throws Exception {
        var service = mock(DishImageService.class);
        MockMvcBuilders.standaloneSetup(new DishImageController(service)).setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/api/v1/dishes/" + UUID.randomUUID() + "/images?" + query)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
