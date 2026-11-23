package com.FoodtimeNeo.common.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestIdFilterTest {
    @AfterEach void clean() { MDC.clear(); }

    @Test void correlatesValidatedBuildAndRequestWithoutLeakingIntoNextRequest() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/dining-halls");
        request.addHeader(RequestIdFilter.HEADER, "request-123");
        request.addHeader(RequestIdFilter.BUILD_HEADER, "abc1234");
        var response = new MockHttpServletResponse();
        new RequestIdFilter().doFilter(request, response, (req, res) -> {
            assertThat(MDC.get("requestId")).isEqualTo("request-123");
            assertThat(MDC.get("clientBuild")).isEqualTo("abc1234");
        });
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("request-123");
        assertThat(MDC.get("clientBuild")).isNull();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test void missingOrMalformedBuildNeverBlocksLegacyClients() throws Exception {
        for (String build : new String[]{null, "unsafe\r\nheader", "x".repeat(65)}) {
            var request = new MockHttpServletRequest("GET", "/api/v1/system/ping");
            if (build != null) request.addHeader(RequestIdFilter.BUILD_HEADER, build);
            var response = new MockHttpServletResponse();
            new RequestIdFilter().doFilter(request, response, (req, res) ->
                    assertThat(MDC.get("clientBuild")).isEqualTo("unknown"));
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test void restoresOuterContextEvenWhenDownstreamFails() {
        MDC.put("requestId", "outer-request");
        MDC.put("clientBuild", "outer-build");
        var request = new MockHttpServletRequest("GET", "/api/v1/system/ping");
        assertThatThrownBy(() -> new RequestIdFilter().doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            throw new IllegalStateException("downstream");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get("requestId")).isEqualTo("outer-request");
        assertThat(MDC.get("clientBuild")).isEqualTo("outer-build");
    }

    @Test void failureLogContainsCorrelationButNotQueryOrRequestBody() throws Exception {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(RequestIdFilter.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("POST", "/api/v1/example");
            request.addHeader(RequestIdFilter.HEADER, "req-failure");
            request.addHeader(RequestIdFilter.BUILD_HEADER, "abc1234");
            request.setQueryString("token=private-query");
            request.setContent("private-password".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            new RequestIdFilter().doFilter(request, new MockHttpServletResponse(), (req, res) ->
                    ((jakarta.servlet.http.HttpServletResponse) res).setStatus(500));
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getFormattedMessage())
                    .contains("POST", "/api/v1/example", "500", "req-failure", "abc1234")
                    .doesNotContain("private-query", "private-password");
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
}
