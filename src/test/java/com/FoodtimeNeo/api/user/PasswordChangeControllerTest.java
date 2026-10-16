package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.api.auth.AuthExceptionHandler;
import com.FoodtimeNeo.auth.dto.ChangePasswordRequest;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.auth.service.PasswordChangeRateLimiter;
import com.FoodtimeNeo.auth.service.PasswordChangeRateLimitException;
import com.FoodtimeNeo.auth.service.PasswordChangeService;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PasswordChangeControllerTest {
    private final PasswordChangeService changes = mock(PasswordChangeService.class);
    private final PasswordChangeRateLimiter limiter = mock(PasswordChangeRateLimiter.class);
    private final SessionAuthenticationService sessions = mock(SessionAuthenticationService.class);
    private final CookieSerializer cookies = mock(CookieSerializer.class);
    private final UserProfile user = new UserProfile(UUID.randomUUID(), "123@bjtu.edu.cn", "干饭人123456", "user", "active", Instant.now());
    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PasswordChangeController(changes, limiter, sessions, cookies))
                .setControllerAdvice(new AuthExceptionHandler(), new GlobalExceptionHandler())
                .defaultRequest(put("/").requestAttr(SessionAuthenticationService.CURRENT_USER, user)).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1A", "abcdefgh", "12345678", "Abc12345 ", "Abc12345\n", "Abc12345中文", "Abc12345😀"})
    void invalidNewPasswordsNeverReachRateLimiterOrService(String password) throws Exception {
        mvc.perform(put("/api/v1/users/me/password").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("oldPassword", "Old12345!", "newPassword", password))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(changes, limiter, sessions, cookies);
    }

    @Test
    void missingAndOverlongFieldsAreRejected() throws Exception {
        for (String payload : new String[] {"{}", "{\"oldPassword\":\"Old12345!\"}",
                json.writeValueAsString(Map.of("oldPassword", "Old12345!", "newPassword", "A1" + "x".repeat(127))),
                json.writeValueAsString(Map.of("oldPassword", " Old12345!", "newPassword", "New12345!"))}) {
            mvc.perform(put("/api/v1/users/me/password").contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(changes, limiter, sessions, cookies);
    }

    @Test
    void successUsesOnlyAuthenticatedUserAndExpiresCookie() throws Exception {
        mvc.perform(put("/api/v1/users/me/password").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("oldPassword", "Old12345!", "newPassword", "New12345!", "userId", UUID.randomUUID(), "role", "superadmin"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Old12345!"))));
        verify(changes).change(user, new ChangePasswordRequest("Old12345!", "New12345!"));
        verify(sessions).clear(any());
        var cookie = org.mockito.ArgumentCaptor.forClass(CookieSerializer.CookieValue.class);
        verify(cookies).writeCookieValue(cookie.capture());
        assertThat(cookie.getValue().getCookieMaxAge()).isZero();
        assertThat(cookie.getValue().getCookieValue()).isEmpty();
    }

    @Test
    void committedPasswordIsNotReportedAsFailureIfSessionCleanupFails() throws Exception {
        doThrow(new RedisConnectionFailureException("private-redis")).when(sessions).clear(any());
        mvc.perform(put("/api/v1/users/me/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"Old12345!\",\"newPassword\":\"New12345!\"}"))
                .andExpect(status().isOk());
        verify(cookies).writeCookieValue(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void businessFailurePreservesSession() throws Exception {
        doThrow(new BusinessException("OLD_PASSWORD_INCORRECT", "旧密码错误", HttpStatus.BAD_REQUEST))
                .when(changes).change(any(), any());
        mvc.perform(put("/api/v1/users/me/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"Wrong123!\",\"newPassword\":\"New12345!\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OLD_PASSWORD_INCORRECT"));
        verifyNoInteractions(sessions, cookies);
    }

    @Test
    void limitReturnsRetryAfterAndStopsService() throws Exception {
        doThrow(new PasswordChangeRateLimitException(90)).when(limiter).check(any(), any());
        mvc.perform(put("/api/v1/users/me/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"Old12345!\",\"newPassword\":\"New12345!\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "90"))
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_RATE_LIMITED"));
        verifyNoInteractions(changes, sessions, cookies);
    }

    @Test
    void dtoDoesNotSerializeOrLogEitherPassword() {
        var body = new ChangePasswordRequest("Old12345!", "New12345!");
        assertThat(body.toString()).doesNotContain(body.oldPassword(), body.newPassword());
        assertThat(json.writeValueAsString(body)).isEqualTo("{}");
    }
}
