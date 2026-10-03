package com.FoodtimeNeo.common.web;

import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.api.system.SystemController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebInfrastructureTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SystemController(), new ValidationTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void responseAndLogsShareARequestIdWithoutLeakingItToTheNextRequest() throws Exception {
        mvc.perform(get("/api/v1/system/ping").header("X-Request-Id", "client-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "client-123"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.requestId").value("client-123"))
                .andExpect(jsonPath("$.data.status").value("UP"));
        assertThat(MDC.get("requestId")).isNull();
        mvc.perform(get("/api/v1/system/ping").header("X-Request-Id", "unsafe id"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", not("unsafe id")))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void unsupportedMethodKeepsTheHttpStatusAndAllowHeader() throws Exception {
        mvc.perform(post("/api/v1/system/ping"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void invalidJsonDoesNotExposeParserOrImplementationDetails() throws Exception {
        mvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void validationErrorsReturnFieldNamesWithoutRejectedValues() throws Exception {
        mvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data[0].field").value("name"))
                .andExpect(jsonPath("$.data[0].rejectedValue").doesNotExist());
    }

    @RestController
    static class ValidationTestController {
        @PostMapping("/test/validation")
        Object validate(@Valid @RequestBody TestInput input) {
            return input;
        }
    }

    record TestInput(@NotBlank String name) { }
}
