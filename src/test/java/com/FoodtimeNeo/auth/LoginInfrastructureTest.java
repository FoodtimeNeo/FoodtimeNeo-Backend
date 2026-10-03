package com.FoodtimeNeo.auth;

import com.FoodtimeNeo.auth.security.SecurityErrorWriter;
import com.FoodtimeNeo.auth.security.SessionInfrastructureFilter;
import com.FoodtimeNeo.auth.service.LoginRateLimiter;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginInfrastructureTest {
    @Test
    void redisFailureNeverBypassesTheLimiter() {
        var redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("private connection data"));
        var limiter = new LoginRateLimiter(redis, new AuthProperties(Duration.ofDays(7), false,
                Duration.ofMinutes(15), 10, 100, "test:auth"));
        assertThatThrownBy(() -> limiter.check("123@bjtu.edu.cn", "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getStatus().value()).isEqualTo(503);
                    assertThat(error.getCode()).isEqualTo("AUTH_UNAVAILABLE");
                    assertThat(error.getMessage()).doesNotContain("private connection data");
                });
    }

    @Test
    void sessionInfrastructureErrorsOutsideMvcStillProduceSafe503Json() throws Exception {
        var filter = new SessionInfrastructureFilter(new SecurityErrorWriter(JsonMapper.builder().build()));
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response,
                (request, result) -> { throw new RedisConnectionFailureException("private Redis settings"); });
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("AUTH_UNAVAILABLE").doesNotContain("private Redis settings");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }
}
