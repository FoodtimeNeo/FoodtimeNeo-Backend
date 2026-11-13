package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.authz.CapabilityService;
import com.FoodtimeNeo.authz.CapabilitySnapshot;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.common.web.RequestIdFilter;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CapabilityControllerTest {
    @Test
    void returnsTheServerProjectedCapabilities() throws Exception {
        CapabilityService capabilities = mock(CapabilityService.class);
        UserProfile actor = new UserProfile(UUID.randomUUID(), "admin@bjtu.edu.cn", "管理员",
                "admin", "active", Instant.now());
        when(capabilities.snapshot(any())).thenReturn(
                new CapabilitySnapshot(List.of("catalog.edit", "correction.submit", "moderation.review"), "default-v2"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CapabilityController(capabilities))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();

        MockHttpServletRequestBuilder request = get("/api/v1/users/me/capabilities")
                .requestAttr(SessionAuthenticationService.CURRENT_USER, actor)
                .header("X-Request-Id", "capability-test");
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.capabilities[0]").value("catalog.edit"))
                .andExpect(jsonPath("$.data.capabilities[1]").value("correction.submit"))
                .andExpect(jsonPath("$.data.capabilities[2]").value("moderation.review"))
                .andExpect(jsonPath("$.data.revision").value("default-v2"))
                .andExpect(jsonPath("$.requestId").value("capability-test"));
    }
}
