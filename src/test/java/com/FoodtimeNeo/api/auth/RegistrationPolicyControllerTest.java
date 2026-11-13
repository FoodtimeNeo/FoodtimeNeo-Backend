package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.dto.RegistrationPolicyResponse;
import com.FoodtimeNeo.auth.service.RegistrationPolicyService;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.common.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistrationPolicyControllerTest {
    @Test
    void returnsTheStableCapabilitySet() throws Exception {
        RegistrationPolicyService policies = mock(RegistrationPolicyService.class);
        when(policies.current()).thenReturn(
                new RegistrationPolicyResponse("invitation", true, true, "invitation:email"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RegistrationPolicyController(policies))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();

        mvc.perform(get("/api/v1/auth/registration-policy").header("X-Request-Id", "policy-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.mode").value("invitation"))
                .andExpect(jsonPath("$.data.canSendEmailCode").value(true))
                .andExpect(jsonPath("$.data.invitationRequired").value(true))
                .andExpect(jsonPath("$.data.revision").value("invitation:email"))
                .andExpect(jsonPath("$.requestId").value("policy-test"));
    }
}
