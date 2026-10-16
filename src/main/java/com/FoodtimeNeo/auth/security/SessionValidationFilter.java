package com.FoodtimeNeo.auth.security;

import com.FoodtimeNeo.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Installed only inside SecurityFilterChain, after context loading and before CSRF/authorization. */
public class SessionValidationFilter extends OncePerRequestFilter {
    private final SessionAuthenticationService sessions;
    private final SecurityErrorWriter errors;
    public SessionValidationFilter(SessionAuthenticationService sessions, SecurityErrorWriter errors) {
        this.sessions = sessions;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal) {
            try {
                sessions.validate(principal, request);
            } catch (BusinessException exception) {
                SecurityContextHolder.clearContext();
                errors.write(response, exception.getStatus().value(), exception.getCode(), exception.getMessage());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
