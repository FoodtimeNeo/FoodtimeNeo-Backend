package com.FoodtimeNeo.api.dining;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.common.web.RequestIdFilter;
import com.FoodtimeNeo.dining.dto.DiningHallResponse;
import com.FoodtimeNeo.dining.service.DiningHallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DiningHallControllerTest {
    private final DiningHallService halls = mock(DiningHallService.class);
    private MockMvc mvc;
    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DiningHallController(halls))
                .setControllerAdvice(new GlobalExceptionHandler()).addFilters(new RequestIdFilter()).build();
    }
    @Test
    void returnsPublicFieldsAndRequestIdWithoutInternalMetadata() throws Exception {
        UUID id = UUID.randomUUID();
        when(halls.list()).thenReturn(List.of(new DiningHallResponse(id, "第一食堂", null, null, null, null)));
        mvc.perform(get("/api/v1/dining-halls").header("X-Request-Id", "hall-list-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].name").value("第一食堂"))
                .andExpect(jsonPath("$.data[0].sortOrder").doesNotExist())
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(header().string("X-Request-Id", "hall-list-test"));
    }
    @Test
    void noHallsReturnsArrayInsteadOfNullOrNotFound() throws Exception {
        when(halls.list()).thenReturn(List.of());
        mvc.perform(get("/api/v1/dining-halls")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray()).andExpect(jsonPath("$.data").isEmpty());
    }
    @Test
    void unavailableServiceRetains503AndBusinessCode() throws Exception {
        when(halls.list()).thenThrow(new BusinessException("DINING_HALLS_UNAVAILABLE", "食堂列表服务暂时不可用，请稍后重试",
                HttpStatus.SERVICE_UNAVAILABLE));
        mvc.perform(get("/api/v1/dining-halls")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DINING_HALLS_UNAVAILABLE"));
    }
}
