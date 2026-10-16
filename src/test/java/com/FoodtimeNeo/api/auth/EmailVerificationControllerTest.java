package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.dto.EmailCodeResponse;
import com.FoodtimeNeo.auth.verification.EmailVerificationService;
import com.FoodtimeNeo.auth.verification.EmailRateLimitException;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EmailVerificationControllerTest {
    private final EmailVerificationService service = mock(EmailVerificationService.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;
    private static final String PATH = "/api/v1/auth/register/email-code";

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new EmailVerificationController(service))
                .setControllerAdvice(new AuthExceptionHandler(), new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc@bjtu.edu.cn", "123@outside.example", "123@bjtu.edu.cn.evil.test", "123\r\nBcc:attacker@example.com"})
    void invalidAddressesNeverReachTheMailSender(String email) throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @Test
    void normalizedAddressReturnsExpiryAndNoCode() throws Exception {
        when(service.send(any(), anyString())).thenReturn(new EmailCodeResponse(Instant.parse("2026-10-04T01:15:00Z"), 60));
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", " 00123@BJTU.EDU.CN "))))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.resendAfterSeconds").value(60))
                .andExpect(jsonPath("$.data.verificationCode").doesNotExist());
        verify(service).send(new com.FoodtimeNeo.auth.dto.EmailCodeRequest("00123@bjtu.edu.cn"), "127.0.0.1");
    }

    @Test
    void limiterReturnsRetryAfterWithoutExposingInternals() throws Exception {
        when(service.send(any(), anyString())).thenThrow(new EmailRateLimitException(45));
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"123@bjtu.edu.cn\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "45"))
                .andExpect(jsonPath("$.code").value("EMAIL_SEND_RATE_LIMITED"));
    }
}
