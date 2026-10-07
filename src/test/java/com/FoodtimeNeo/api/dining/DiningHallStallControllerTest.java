package com.FoodtimeNeo.api.dining;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.common.web.RequestIdFilter;
import com.FoodtimeNeo.dining.dto.StallResponse;
import com.FoodtimeNeo.dining.service.StallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DiningHallStallControllerTest {
    private final StallService service = mock(StallService.class);
    private MockMvc mvc;
    private final UUID hall = UUID.randomUUID();
    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DiningHallStallController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).addFilters(new RequestIdFilter()).build();
    }
    @Test
    void usesPathHallAndReturnsPublicFieldsWithRequestId() throws Exception {
        UUID stall = UUID.randomUUID();
        when(service.listByDiningHall(hall)).thenReturn(List.of(new StallResponse(stall, hall, "第一档口", null, null, null)));
        mvc.perform(get("/api/v1/dining-halls/{id}/stalls", hall).header("X-Request-Id", "stall-list-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(stall.toString()))
                .andExpect(jsonPath("$.data[0].diningHallId").value(hall.toString()))
                .andExpect(jsonPath("$.data[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[0].sortOrder").doesNotExist())
                .andExpect(header().string("X-Request-Id", "stall-list-test"));
        verify(service).listByDiningHall(hall);
    }
    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "123", "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz"})
    void malformedHallIdFailsBeforeCallingService(String id) throws Exception {
        mvc.perform(get("/api/v1/dining-halls/{id}/stalls", id))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }
    @Test
    void activeHallWithoutStallsReturns200EmptyArray() throws Exception {
        when(service.listByDiningHall(hall)).thenReturn(List.of());
        mvc.perform(get("/api/v1/dining-halls/{id}/stalls", hall))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray()).andExpect(jsonPath("$.data").isEmpty());
    }
    @Test
    void notFoundKeeps404AndDomainCode() throws Exception {
        when(service.listByDiningHall(hall)).thenThrow(new BusinessException("DINING_HALL_NOT_FOUND", "食堂不存在或暂不可用", HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/v1/dining-halls/{id}/stalls", hall))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("DINING_HALL_NOT_FOUND"));
    }
    @Test
    void unavailableKeeps503AndDomainCode() throws Exception {
        when(service.listByDiningHall(hall)).thenThrow(new BusinessException("STALLS_UNAVAILABLE", "档口列表服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE));
        mvc.perform(get("/api/v1/dining-halls/{id}/stalls", hall))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("STALLS_UNAVAILABLE"));
    }
}
