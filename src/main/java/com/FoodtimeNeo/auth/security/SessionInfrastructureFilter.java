package com.FoodtimeNeo.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Session loading/saving happens outside MVC exception handlers; translate infrastructure failures here. */
@Component
@Order(SessionRepositoryFilter.DEFAULT_ORDER - 1)
public class SessionInfrastructureFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(SessionInfrastructureFilter.class);
    private final SecurityErrorWriter errors;
    public SessionInfrastructureFilter(SecurityErrorWriter errors) { this.errors = errors; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (DataAccessException exception) {
            SecurityContextHolder.clearContext();
            LOG.error("Authentication infrastructure failed: {}", exception.getClass().getSimpleName());
            errors.write(response, 503, "AUTH_UNAVAILABLE", "登录服务暂时不可用，请稍后重试");
        }
    }
}
