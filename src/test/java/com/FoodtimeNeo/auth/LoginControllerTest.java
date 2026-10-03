package com.FoodtimeNeo.auth;

import com.FoodtimeNeo.auth.controller.LoginController;
import com.FoodtimeNeo.auth.controller.AuthExceptionHandler;
import com.FoodtimeNeo.auth.dto.LoginRequest;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.auth.service.LoginService;
import com.FoodtimeNeo.auth.service.LoginRateLimiter;
import com.FoodtimeNeo.auth.service.LoginRateLimitException;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.common.web.RequestIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LoginControllerTest {
    private final LoginService logins = mock(LoginService.class);
    private final LoginRateLimiter limiter = mock(LoginRateLimiter.class);
    private final SessionAuthenticationService sessions = mock(SessionAuthenticationService.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new LoginController(logins, limiter, sessions))
                .setControllerAdvice(new AuthExceptionHandler(), new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1A", "Abc12345😀", "Abc12345 ", "Abc12345中文", "Abc12345\n"})
    void invalidPasswordsNeverReachTheLimiterOrPasswordHasher(String password) throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("account", "123@bjtu.edu.cn", "password", password))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(limiter, logins, sessions);
    }

    @Test
    void malformedUnicodeReturns400BeforeHashing() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"123@bjtu.edu.cn\",\"password\":\"Abc12345\\ud800\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(limiter, logins, sessions);
    }

    @Test
    void throttlingReturns429WithRetryAfterAndDoesNotHashPasswords() throws Exception {
        doThrow(new LoginRateLimitException(60)).when(limiter).check("123@bjtu.edu.cn", "127.0.0.1");
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginBody(" 123@BJTU.EDU.CN ", "Abc12345"))))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMITED"));
        verifyNoInteractions(logins, sessions);
    }

    private record LoginBody(String account, String password) { }
}
