package com.FoodtimeNeo.auth.security;

import com.FoodtimeNeo.auth.service.LoginService;
import com.FoodtimeNeo.config.AuthProperties;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.List;

@Service
public class SessionAuthenticationService {
    public static final String CURRENT_USER = SessionAuthenticationService.class.getName() + ".user";
    private final UserMapper users;
    private final SecurityContextRepository contexts;
    private final AuthProperties properties;
    private final Clock clock;

    public SessionAuthenticationService(UserMapper users, SecurityContextRepository contexts,
                                        AuthProperties properties, Clock clock) {
        this.users = users;
        this.contexts = contexts;
        this.properties = properties;
        this.clock = clock;
    }

    public Instant establish(UserProfile user, HttpServletRequest request, HttpServletResponse response) {
        // Start a new session without copying anonymous attributes or the pre-login CSRF token.
        clear(request);
        Instant expiresAt = clock.instant().plus(properties.sessionTtl());
        request.getSession(true).setMaxInactiveInterval(Math.toIntExact(properties.sessionTtl().toSeconds()));
        var principal = new SessionPrincipal(user.id(), user.passwordChangedAt(), expiresAt);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication(principal, user.role()));
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return expiresAt;
    }

    public void validate(SessionPrincipal principal, HttpServletRequest request) {
        if (!clock.instant().isBefore(principal.expiresAt())) {
            clear(request);
            return;
        }
        UserProfile user;
        try {
            user = users.findProfile(principal.userId());
        } catch (DataAccessException exception) {
            throw LoginService.unavailable();
        }
        if (user == null || !"active".equals(user.status())
                || !user.passwordChangedAt().equals(principal.passwordChangedAt())) {
            clear(request);
            return;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            SecurityContextHolder.clearContext();
            return;
        }
        Instant now = clock.instant();
        if (!now.isBefore(principal.expiresAt())) {
            clear(request);
            return;
        }
        // Spring Session normally uses sliding idle time. Bound it by the absolute login deadline.
        long remaining = Math.max(1, Duration.between(now, principal.expiresAt()).toSeconds());
        session.setMaxInactiveInterval(Math.toIntExact(remaining));
        SecurityContextHolder.getContext().setAuthentication(authentication(principal, user.role()));
        request.setAttribute(CURRENT_USER, user);
    }

    public void clear(HttpServletRequest request) {
        HttpSession old = request.getSession(false);
        if (old != null) { old.invalidate(); }
        SecurityContextHolder.clearContext();
    }

    private static UsernamePasswordAuthenticationToken authentication(SessionPrincipal principal, String role) {
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
