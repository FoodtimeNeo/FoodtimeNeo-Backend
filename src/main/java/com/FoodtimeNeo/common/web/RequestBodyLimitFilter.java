package com.FoodtimeNeo.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Caps API request bodies at the container boundary.
 * <p>
 * Tomcat's {@code max-http-form-post-size} only guards form posts, so a JSON body is otherwise
 * unlimited and a field that forgets {@code @Size} could stream a gigabyte into a TEXT column.
 * The filter rejects an oversized declared length up front, then re-counts the bytes as they are
 * read because a chunked request omits {@code Content-Length}. Multipart requests keep the
 * existing {@code spring.servlet.multipart.max-request-size} budget.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RequestBodyLimitFilter extends OncePerRequestFilter {
    private final long maxJsonBytes;
    private final long maxMultipartBytes;

    public RequestBodyLimitFilter(@Value("${app.request.max-json-bytes:1048576}") long maxJsonBytes,
                                  @Value("${app.request.max-multipart-bytes:20971520}") long maxMultipartBytes) {
        if (maxJsonBytes < 1 || maxMultipartBytes < 1) {
            throw new IllegalArgumentException("Request body limits must be positive");
        }
        this.maxJsonBytes = maxJsonBytes;
        this.maxMultipartBytes = maxMultipartBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long limit = isMultipart(request) ? maxMultipartBytes : maxJsonBytes;
        if (request.getContentLengthLong() > limit) {
            writeTooLarge(response, limit);
            return;
        }
        // A chunked body reports -1 above, so the stream wrapper enforces the real length.
        filterChain.doFilter(new LimitedRequest(request, limit), response);
    }

    /** Only API traffic is capped; static assets, Swagger UI and actuator keep their own limits. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    private static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(java.util.Locale.ROOT)
                .startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    private static void writeTooLarge(HttpServletResponse response, long limit) throws IOException {
        response.reset();
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("X-Max-Body-Bytes", Long.toString(limit));
        response.getWriter().write("{\"code\":\"PAYLOAD_TOO_LARGE\",\"message\":\"请求内容超出大小限制\","
                + "\"data\":null,\"timestamp\":\"" + java.time.Instant.now() + "\",\"requestId\":"
                + jsonString(org.slf4j.MDC.get("requestId")) + "}");
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {
        private final long limit;
        private LimitedRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new LimitedStream(super.getInputStream(), limit);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), charset(this)));
        }
    }

    private static Charset charset(HttpServletRequest request) {
        String encoding = request.getCharacterEncoding();
        return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    }

    /** Counts consumed bytes and aborts the read once the limit would be exceeded. */
    private static final class LimitedStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final long limit;
        private long consumed;

        private LimitedStream(ServletInputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value != -1 && ++consumed > limit) {
                throw new PayloadTooLargeSignal();
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = delegate.read(buffer, offset, length);
            if (read > 0 && (consumed += read) > limit) {
                throw new PayloadTooLargeSignal();
            }
            return read;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }

        @Override
        public int available() throws IOException {
            return delegate.available();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }

    /** Signals an oversized body so the request is answered with 413 instead of 500. */
    public static final class PayloadTooLargeSignal extends IOException {
        private static final long serialVersionUID = 1L;

        public PayloadTooLargeSignal() {
            super("Request body exceeds the configured limit", null);
        }
    }
}
