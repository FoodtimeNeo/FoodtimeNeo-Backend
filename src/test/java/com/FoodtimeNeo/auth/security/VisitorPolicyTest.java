package com.FoodtimeNeo.auth.security;

import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorPolicyTest {
    private static UserProfile user(String role) {
        return new UserProfile(UUID.randomUUID(), "a@example.com", "访客", role, "active", null);
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }

    @Test
    void visitorsMayReadAnything() {
        assertThat(VisitorPolicy.blocked(user("visitor"), request("GET", "/api/v1/community/feed"))).isFalse();
    }

    @Test
    void visitorsCannotPublishContent() {
        assertThat(VisitorPolicy.blocked(user("visitor"), request("POST", "/api/v1/community/posts"))).isTrue();
        assertThat(VisitorPolicy.blocked(user("visitor"), request("POST", "/api/v1/dishes/x/reviews"))).isTrue();
        assertThat(VisitorPolicy.blocked(user("visitor"), request("PUT", "/api/v1/posts/x/like"))).isTrue();
        assertThat(VisitorPolicy.blocked(user("visitor"), request("POST", "/api/v1/uploads/images"))).isTrue();
    }

    @Test
    void visitorsKeepAccountLevelWrites() {
        assertThat(VisitorPolicy.blocked(user("visitor"), request("POST", "/api/v1/auth/logout"))).isFalse();
        assertThat(VisitorPolicy.blocked(user("visitor"), request("POST", "/api/v1/auth/bind-email/email-code"))).isFalse();
        assertThat(VisitorPolicy.blocked(user("visitor"), request("POST", "/api/v1/users/me/bind-email"))).isFalse();
        assertThat(VisitorPolicy.blocked(user("visitor"), request("PUT", "/api/v1/users/me/password"))).isFalse();
        assertThat(VisitorPolicy.blocked(user("visitor"), request("PUT", "/api/v1/users/me/profile"))).isFalse();
        assertThat(VisitorPolicy.blocked(user("visitor"), request("POST", "/api/v1/users/me/notices/abc/read"))).isFalse();
        assertThat(VisitorPolicy.blocked(user("visitor"), request("POST", "/api/v1/users/me/messages/read-all"))).isFalse();
    }

    @Test
    void visitorsMayReadOrHideDirectMessagesButCannotSend() {
        String id = "2f9b7c1e-3a4d-4e5f-8a9b-0c1d2e3f4a5b";
        assertThat(VisitorPolicy.blocked(user("visitor"),
                request("POST", "/api/v1/users/me/conversations/" + id + "/read"))).isFalse();
        assertThat(VisitorPolicy.blocked(user("visitor"),
                request("DELETE", "/api/v1/users/me/conversations/" + id))).isFalse();
        assertThat(VisitorPolicy.blocked(user("visitor"),
                request("POST", "/api/v1/users/me/conversations/" + id + "/messages"))).isTrue();
        assertThat(VisitorPolicy.blocked(user("visitor"),
                request("POST", "/api/v1/users/me/conversations"))).isTrue();
    }

    @Test
    void regularUsersAreNeverBlocked() {
        assertThat(VisitorPolicy.blocked(user("user"), request("POST", "/api/v1/community/posts"))).isFalse();
        assertThat(VisitorPolicy.blocked(user("admin"), request("POST", "/api/v1/community/posts"))).isFalse();
    }
}
