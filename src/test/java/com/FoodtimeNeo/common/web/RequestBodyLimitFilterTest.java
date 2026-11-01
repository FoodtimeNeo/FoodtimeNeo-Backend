package com.FoodtimeNeo.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RequestBodyLimitFilterTest {
    private final FilterChain chain = mock(FilterChain.class);

    @Test
    void rejectsDeclaredLengthAboveLimitWithoutCallingTheChain() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType("application/json");
        request.setContent(new byte[2048]);
        var response = new MockHttpServletResponse();

        new RequestBodyLimitFilter(1024, 4096).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("PAYLOAD_TOO_LARGE");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsBodyWithinLimit() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType("application/json");
        request.setContent(new byte[512]);
        var response = new MockHttpServletResponse();

        new RequestBodyLimitFilter(1024, 4096).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void enforcesTheLimitWhileReadingAChunkedBody() {
        // A chunked request reports no declared length, so only the stream guard can catch it.
        var chunked = new MockHttpServletRequest("POST", "/api/v1/auth/login") {
            @Override public long getContentLengthLong() { return -1; }
        };
        chunked.setContentType("application/json");
        chunked.setContent(new byte[4096]);
        var filter = new RequestBodyLimitFilter(1024, 4096);
        FilterChain reading = (req, resp) -> req.getInputStream().readAllBytes();

        assertThatThrownBy(() -> filter.doFilter(chunked, new MockHttpServletResponse(), reading))
                .isInstanceOf(RequestBodyLimitFilter.PayloadTooLargeSignal.class);
    }

    @Test
    void multipartUsesTheLargerLimit() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/uploads/images");
        request.setContentType("multipart/form-data; boundary=x");
        request.setContent(new byte[2048]);
        var response = new MockHttpServletResponse();

        new RequestBodyLimitFilter(1024, 4096).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void ignoresNonApiTraffic() throws Exception {
        var request = new MockHttpServletRequest("POST", "/swagger-ui/index.html");
        request.setContentType("application/json");
        request.setContent(new byte[8192]);
        var response = new MockHttpServletResponse();

        new RequestBodyLimitFilter(1024, 4096).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());
    }
}
