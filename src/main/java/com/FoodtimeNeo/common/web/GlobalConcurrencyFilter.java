package com.FoodtimeNeo.common.web;

import com.FoodtimeNeo.auth.security.SecurityErrorWriter;
import com.FoodtimeNeo.config.properties.RequestProtectionProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Caps in-flight API work before it reaches Tomcat threads, SMTP, Argon2 or the small DB pool.
 * <p>
 * The global semaphore is the crash guard. The per-path semaphores are deliberately lower for the
 * three expensive auth paths, so a burst cannot occupy every global slot with SMTP probes or
 * password hashing. Rejections are immediate (no queueing) and return 429 with Retry-After.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 4)
public class GlobalConcurrencyFilter extends OncePerRequestFilter {
    private static final String EMAIL_CODE = "/api/v1/auth/register/email-code";
    private static final String REGISTER = "/api/v1/auth/register";
    private static final String LOGIN = "/api/v1/auth/login";

    private final boolean enabled;
    private final int retryAfterSeconds;
    private final SecurityErrorWriter errors;
    private final Semaphore global;
    private final Semaphore emailCode;
    private final Semaphore registration;
    private final Semaphore login;

    public GlobalConcurrencyFilter(RequestProtectionProperties properties, SecurityErrorWriter errors) {
        this.enabled = properties.enabled();
        this.retryAfterSeconds = properties.retryAfterSeconds();
        this.errors = errors;
        this.global = new Semaphore(properties.globalMaxConcurrency());
        this.emailCode = new Semaphore(properties.emailCodeMaxConcurrency());
        this.registration = new Semaphore(properties.registrationMaxConcurrency());
        this.login = new Semaphore(properties.loginMaxConcurrency());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        // Health/monitoring must remain answerable while the API is under pressure.
        return !path.startsWith("/api/v1/") || "/api/v1/system/ping".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Semaphore pathLimit = pathLimit(request.getRequestURI());
        if (!global.tryAcquire()) {
            reject(response);
            return;
        }
        boolean pathAcquired = pathLimit == null || pathLimit.tryAcquire();
        if (!pathAcquired) {
            global.release();
            reject(response);
            return;
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (pathAcquired && pathLimit != null) {
                pathLimit.release();
            }
            global.release();
        }
    }

    private Semaphore pathLimit(String path) {
        return switch (path) {
            case EMAIL_CODE -> emailCode;
            case REGISTER -> registration;
            case LOGIN -> login;
            default -> null;
        };
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setHeader("Retry-After", Integer.toString(retryAfterSeconds));
        errors.write(response, 429, "SERVER_BUSY", "服务器繁忙，请稍后重试");
    }
}
