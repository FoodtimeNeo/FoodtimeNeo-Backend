package com.FoodtimeNeo.common.web;

import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.config.properties.ClientHeaderMode;
import com.FoodtimeNeo.config.properties.ClientHeaderProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Rejects calls to the guarded paths that do not name an official client.
 * <p>
 * This raises the cost of a bare script; it does not prove the caller is genuine, because both the
 * web bundle and the APK can be inspected and replayed. Enforcement therefore defaults to observe.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class ClientHeaderFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ClientHeaderFilter.class);

    private final ClientHeaderProperties properties;

    public ClientHeaderFilter(ClientHeaderProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (properties.mode() == ClientHeaderMode.OFF) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return properties.paths().stream().noneMatch(path::equals);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String supplied = request.getHeader(properties.headerName());
        boolean recognized = isRecognized(supplied);
        if (!recognized) {
            LOG.warn("Unrecognized client on {} {}: header={} requestId={}", request.getMethod(),
                    request.getRequestURI(), sanitize(supplied), MDC.get("requestId"));
            if (properties.mode() == ClientHeaderMode.ENFORCE) {
                reject(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRecognized(String supplied) {
        if (supplied == null) {
            return false;
        }
        String value = supplied.strip().toLowerCase(Locale.ROOT);
        return properties.allowedClients().stream()
                .anyMatch(allowed -> allowed.strip().toLowerCase(Locale.ROOT).equals(value));
    }

    /** Header values end up in logs; keep them short and free of control characters. */
    private static String sanitize(String value) {
        if (value == null) {
            return "<absent>";
        }
        String cleaned = value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        return cleaned.length() <= 40 ? cleaned : cleaned.substring(0, 40) + "...";
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.error("CLIENT_UNKNOWN", "请使用官方客户端访问", null);
        response.getWriter().write(String.format(
                "{\"code\":\"%s\",\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\",\"requestId\":%s}",
                body.code(), body.message(), body.timestamp(), jsonString(body.requestId())));
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
