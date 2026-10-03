package com.FoodtimeNeo.auth;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.auth.security.SessionPrincipal;
import com.FoodtimeNeo.config.AuthProperties;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import java.time.*;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionAuthenticationServiceTest {
    private final Instant now = Instant.parse("2026-10-04T00:00:00Z");
    private final UserMapper users = mock(UserMapper.class);
    private final UserProfile user = new UserProfile(UUID.randomUUID(), "123@bjtu.edu.cn", "干饭人123456", "user",
            "active", now.minusSeconds(100));
    private final SessionAuthenticationService sessions = new SessionAuthenticationService(users,
            new HttpSessionSecurityContextRepository(), new AuthProperties(Duration.ofDays(7), false,
            Duration.ofMinutes(15), 10, 100, "test:auth"), Clock.fixed(now, ZoneOffset.UTC));

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void successfulLoginReplacesAnonymousSessionAndStoresNoCredentials() {
        var request = new MockHttpServletRequest();
        var anonymous = request.getSession();
        anonymous.setAttribute("old-anonymous-state", "must-not-survive");
        String oldId = anonymous.getId();
        Instant expiry = sessions.establish(user, request, new MockHttpServletResponse());
        assertThat(expiry).isEqualTo(now.plus(Duration.ofDays(7)));
        assertThat(request.getSession().getId()).isNotEqualTo(oldId);
        assertThat(request.getSession().getAttribute("old-anonymous-state")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(new SessionPrincipal(user.id(), user.passwordChangedAt(), expiry));
    }

    @Test
    void activeRequestsUseRemainingLifetimeInsteadOfRestartingSevenDays() {
        var request = authenticatedRequest();
        var principal = new SessionPrincipal(user.id(), user.passwordChangedAt(), now.plusSeconds(60));
        when(users.findProfile(user.id())).thenReturn(user);
        sessions.validate(principal, request);
        assertThat(request.getSession().getMaxInactiveInterval()).isEqualTo(60);
        assertThat(request.getAttribute(SessionAuthenticationService.CURRENT_USER)).isEqualTo(user);
    }

    @Test
    void absoluteExpirationClearsTheSessionWithoutReadingTheDatabase() {
        var request = authenticatedRequest();
        sessions.validate(new SessionPrincipal(user.id(), user.passwordChangedAt(), now), request);
        assertThat(request.getSession(false)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(users);
    }

    @Test
    void passwordChangesAndDisablementInvalidateExistingSessions() {
        for (UserProfile changed : List.of(new UserProfile(user.id(), user.email(), user.displayName(), user.role(),
                "disabled", user.passwordChangedAt()), new UserProfile(user.id(), user.email(), user.displayName(),
                user.role(), "active", now))) {
            var request = authenticatedRequest();
            when(users.findProfile(user.id())).thenReturn(changed);
            sessions.validate(new SessionPrincipal(user.id(), user.passwordChangedAt(), now.plusSeconds(60)), request);
            assertThat(request.getSession(false)).isNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Test
    void roleChangesUseTheDatabaseRatherThanCachedSessionAuthorities() {
        var request = authenticatedRequest();
        when(users.findProfile(user.id())).thenReturn(new UserProfile(user.id(), user.email(), user.displayName(),
                "admin", "active", user.passwordChangedAt()));
        sessions.validate(new SessionPrincipal(user.id(), user.passwordChangedAt(), now.plusSeconds(60)), request);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_admin");
    }

    private MockHttpServletRequest authenticatedRequest() {
        var request = new MockHttpServletRequest();
        request.getSession();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("placeholder", null, List.of()));
        return request;
    }
}
