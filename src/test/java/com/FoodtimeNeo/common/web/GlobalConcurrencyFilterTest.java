package com.FoodtimeNeo.common.web;

import com.FoodtimeNeo.auth.security.SecurityErrorWriter;
import com.FoodtimeNeo.config.properties.RequestProtectionProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GlobalConcurrencyFilterTest {
    private static final String EMAIL_CODE = "/api/v1/auth/register/email-code";
    private static final String REGISTER = "/api/v1/auth/register";

    private static GlobalConcurrencyFilter filter(int global, int emailCode, int registration, int login) {
        return new GlobalConcurrencyFilter(new RequestProtectionProperties(
                true, global, emailCode, registration, login, 2),
                new SecurityErrorWriter(JsonMapper.builder().build()));
    }

    private static MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("POST", path);
    }

    private static FilterChain blocking(CountDownLatch entered, CountDownLatch release) {
        return (request, response) -> {
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        };
    }

    @Test
    void globalLimitRejectsImmediatelyWithRetryAfter() throws Exception {
        GlobalConcurrencyFilter filter = filter(1, 1, 1, 1);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> first = executor.submit(() -> {
                try {
                    filter.doFilter(request(EMAIL_CODE), new MockHttpServletResponse(),
                            blocking(entered, release));
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

            FilterChain secondChain = mock(FilterChain.class);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request(EMAIL_CODE), response, secondChain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isEqualTo("2");
            assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("SERVER_BUSY");
            verifyNoInteractions(secondChain);

            release.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void expensivePathLimitDoesNotBlockOtherApiPaths() throws Exception {
        GlobalConcurrencyFilter filter = filter(10, 1, 10, 10);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> first = executor.submit(() -> {
                try {
                    filter.doFilter(request(EMAIL_CODE), new MockHttpServletResponse(),
                            blocking(entered, release));
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

            FilterChain emailChain = mock(FilterChain.class);
            MockHttpServletResponse emailResponse = new MockHttpServletResponse();
            filter.doFilter(request(EMAIL_CODE), emailResponse, emailChain);
            assertThat(emailResponse.getStatus()).isEqualTo(429);
            verifyNoInteractions(emailChain);

            FilterChain registrationChain = mock(FilterChain.class);
            filter.doFilter(request(REGISTER), new MockHttpServletResponse(), registrationChain);
            verify(registrationChain).doFilter(any(), any());

            release.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void unlistedApiPathUsesOnlyTheGlobalLimit() throws Exception {
        GlobalConcurrencyFilter filter = filter(2, 1, 1, 1);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(request("/api/v1/dining-halls"), first, chain);
        assertThat(first.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());

        // The global permit from the unlisted path must be released for the next request.
        FilterChain secondChain = mock(FilterChain.class);
        filter.doFilter(request("/api/v1/dining-halls"), new MockHttpServletResponse(), secondChain);
        verify(secondChain).doFilter(any(), any());
    }

    @Test
    void healthAndPreflightBypassTheGlobalLimit() throws Exception {
        GlobalConcurrencyFilter filter = filter(1, 1, 1, 1);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> first = executor.submit(() -> {
                try {
                    filter.doFilter(request(EMAIL_CODE), new MockHttpServletResponse(),
                            blocking(entered, release));
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

            FilterChain pingChain = mock(FilterChain.class);
            MockHttpServletRequest ping = new MockHttpServletRequest("GET", "/api/v1/system/ping");
            filter.doFilter(ping, new MockHttpServletResponse(), pingChain);
            verify(pingChain).doFilter(any(), any());

            FilterChain preflightChain = mock(FilterChain.class);
            MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", REGISTER);
            filter.doFilter(preflight, new MockHttpServletResponse(), preflightChain);
            verify(preflightChain).doFilter(any(), any());

            release.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void disabledProtectionLetsRequestsThrough() throws Exception {
        GlobalConcurrencyFilter filter = new GlobalConcurrencyFilter(new RequestProtectionProperties(
                false, 1, 1, 1, 1, 2), new SecurityErrorWriter(JsonMapper.builder().build()));
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request(EMAIL_CODE), new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
    }
}
