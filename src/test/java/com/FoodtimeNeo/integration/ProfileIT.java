package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rating-summary.refresh-enabled=false")
class ProfileIT extends IntegrationApiSupport {
    @Test
    void profileReadsOnlyTheAuthenticatedUserIncludingNullableAvatar() throws Exception {
        Browser browser = login(owner);
        var empty = body(browser.call("GET", "/api/v1/users/me/profile?userId=" + other.id(), null)).path("data");
        assertThat(empty.path("id").asString()).isEqualTo(owner.id().toString());
        assertThat(empty.path("avatarUrl").isNull()).isTrue();
        jdbc.update("UPDATE foodtime.users SET avatar_url = '/api/v1/media/profile-test' WHERE id = ?", owner.id());
        var response = browser.call("GET", "/api/v1/users/me/profile", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body(response).path("data").path("avatarUrl").asString()).isEqualTo("/api/v1/media/profile-test");
        assertThat(response.body()).doesNotContain("passwordHash", "passwordChangedAt", PASSWORD);
    }

    @Test
    void disabledUsersCannotReuseTheSessionToReadProfile() throws Exception {
        Browser browser = login(owner);
        jdbc.update("UPDATE foodtime.users SET status = 'disabled' WHERE id = ?", owner.id());
        assertThat(browser.call("GET", "/api/v1/users/me/profile", null).statusCode()).isEqualTo(401);
    }
}
