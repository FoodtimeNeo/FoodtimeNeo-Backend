package com.FoodtimeNeo.common.web;

import com.FoodtimeNeo.config.properties.MonitoringProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/** Warns on slow API responses; runs inside the request-id scope for correlation. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SlowRequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(SlowRequestLoggingFilter.class);
    private final MonitoringProperties properties;

    public SlowRequestLoggingFilter(MonitoringProperties properties) { this.properties = properties; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
            if (elapsed.compareTo(properties.slowRequestThreshold()) > 0) {
                LOG.warn("Slow request {} {} took {}ms status={} requestId={}", request.getMethod(),
                        request.getRequestURI(), elapsed.toMillis(), response.getStatus(), MDC.get("requestId"));
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
}
