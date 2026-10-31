package com.FoodtimeNeo.common.web;

import com.FoodtimeNeo.config.properties.ClientHeaderMode;
import com.FoodtimeNeo.config.properties.ClientHeaderProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ClientHeaderFilterTest {
    private static final String REGISTER = "/api/v1/auth/register";
    private static final String HEADER = "X-Foodtime-Client";

    private static ClientHeaderFilter filter(ClientHeaderMode mode) {
        return new ClientHeaderFilter(new ClientHeaderProperties(mode, HEADER,
                List.of("web", "android"), List.of(REGISTER, "/api/v1/auth/register/email-code")));
    }

    private static MockHttpServletRequest post(String path, String client) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        if (client != null) {
            request.addHeader(HEADER, client);
        }
        return request;
    }

    /** Runs the filter with a fresh chain and returns both the response and whether the chain ran. */
    private static Object[] run(ClientHeaderMode mode, MockHttpServletRequest request) throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter(mode).doFilter(request, response, chain);
        return new Object[] {response, chain};
    }

    @Test
    void offModeIgnoresTheHeaderEntirely() throws Exception {
        Object[] result = run(ClientHeaderMode.OFF, post(REGISTER, null));
        assertThat(((MockHttpServletResponse) result[0]).getStatus()).isEqualTo(200);
        verify((FilterChain) result[1]).doFilter(any(), any());
    }

    @Test
    void observeModeLetsUnknownClientsThrough() throws Exception {
        Object[] result = run(ClientHeaderMode.OBSERVE, post(REGISTER, null));
        assertThat(((MockHttpServletResponse) result[0]).getStatus()).isEqualTo(200);
        verify((FilterChain) result[1]).doFilter(any(), any());
    }

    @Test
    void enforceModeRejectsMissingHeader() throws Exception {
        Object[] result = run(ClientHeaderMode.ENFORCE, post(REGISTER, null));
        MockHttpServletResponse response = (MockHttpServletResponse) result[0];
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("CLIENT_UNKNOWN");
        verify((FilterChain) result[1], never()).doFilter(any(), any());
    }

    @Test
    void enforceModeRejectsUnknownClient() throws Exception {
        Object[] result = run(ClientHeaderMode.ENFORCE, post(REGISTER, "curl/8.5.0"));
        assertThat(((MockHttpServletResponse) result[0]).getStatus()).isEqualTo(403);
        verify((FilterChain) result[1], never()).doFilter(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"web", "android", " Web ", "ANDROID"})
    void enforceModeAcceptsOfficialClientsCaseInsensitively(String client) throws Exception {
        Object[] result = run(ClientHeaderMode.ENFORCE, post(REGISTER, client));
        assertThat(((MockHttpServletResponse) result[0]).getStatus()).isEqualTo(200);
        verify((FilterChain) result[1]).doFilter(any(), any());
    }

    @Test
    void unguardedPathsAreNotChecked() throws Exception {
        Object[] result = run(ClientHeaderMode.ENFORCE, post("/api/v1/auth/login", null));
        assertThat(((MockHttpServletResponse) result[0]).getStatus()).isEqualTo(200);
        verify((FilterChain) result[1]).doFilter(any(), any());
    }

    @Test
    void preflightIsNeverBlocked() throws Exception {
        MockHttpServletRequest request = post(REGISTER, null);
        request.setMethod("OPTIONS");
        Object[] result = run(ClientHeaderMode.ENFORCE, request);
        assertThat(((MockHttpServletResponse) result[0]).getStatus()).isEqualTo(200);
        verify((FilterChain) result[1]).doFilter(any(), any());
    }
}
