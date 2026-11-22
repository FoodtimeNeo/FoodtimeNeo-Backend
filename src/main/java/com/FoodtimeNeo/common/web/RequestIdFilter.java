package com.FoodtimeNeo.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Request-Id";
    public static final String BUILD_HEADER = "X-Foodtime-Web-Build";
    private static final Logger LOG = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final Pattern VALID_ID = Pattern.compile("[a-zA-Z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        String incomingId = request.getHeader(HEADER);
        String requestId = incomingId != null && VALID_ID.matcher(incomingId).matches()
                ? incomingId : UUID.randomUUID().toString();
        String previousId = MDC.get("requestId");
        String previousBuild = MDC.get("clientBuild");
        String incomingBuild = request.getHeader(BUILD_HEADER);
        String build = incomingBuild != null && VALID_ID.matcher(incomingBuild).matches() ? incomingBuild : "unknown";
        MDC.put("requestId", requestId);
        MDC.put("clientBuild", build);
        response.setHeader(HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (response.getStatus() >= 500 && request.getRequestURI().startsWith("/api/")) {
                String path = request.getRequestURI().replaceAll("[\\r\\n\\t]", "_");
                if (path.length() > 256) path = path.substring(0, 256);
                LOG.warn("API failure method={} path={} status={} requestId={} clientBuild={}",
                        request.getMethod(), path, response.getStatus(), requestId, build);
            }
            if (previousBuild == null) MDC.remove("clientBuild");
            else MDC.put("clientBuild", previousBuild);
            if (previousId == null) {
                MDC.remove("requestId");
            } else {
                MDC.put("requestId", previousId);
            }
        }
    }
}
