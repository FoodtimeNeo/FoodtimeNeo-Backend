package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.imageio.ImageIO;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileIT extends IntegrationApiSupport {
    @Test
    void profileReadsOnlyTheAuthenticatedUserIncludingNullableAvatar() throws Exception {
        Browser browser = login(owner);
        var empty = body(browser.call("GET", "/api/v1/users/me/profile?userId=" + other.id(), null)).path("data");
        assertThat(empty.path("id").asString()).isEqualTo(owner.id().toString());
        assertThat(empty.path("avatarUrl").isNull()).isTrue();
        jdbc.update("UPDATE foodtime.users SET avatar_url = '/api/v1/media/0a3c0e9a-7405-585e-94ce-29d9275b1b6e' WHERE id = ?", owner.id());
        var response = browser.call("GET", "/api/v1/users/me/profile", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body(response).path("data").path("avatarUrl").asString()).isEqualTo("/api/v1/media/0a3c0e9a-7405-585e-94ce-29d9275b1b6e");
        assertThat(response.body()).doesNotContain("passwordHash", "passwordChangedAt", PASSWORD);
    }

    @Test
    void disabledUsersCannotReuseTheSessionToReadProfile() throws Exception {
        Browser browser = login(owner);
        jdbc.update("UPDATE foodtime.users SET status = 'disabled' WHERE id = ?", owner.id());
        assertThat(browser.call("GET", "/api/v1/users/me/profile", null).statusCode()).isEqualTo(401);
    }

    @Test
    void updateNicknameAndOwnAvatar() throws Exception {
        Browser browser = login(owner);
        String imageUrl = body(browser.upload(png())).path("data").path("imageUrl").asString();
        var response = browser.call("PUT", "/api/v1/users/me/profile",
                Map.of("displayName", "干饭达人", "avatarUrl", imageUrl));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body(response).path("data").path("displayName").asString()).isEqualTo("干饭达人");
        assertThat(body(response).path("data").path("avatarUrl").asString()).isEqualTo(imageUrl);
        var reread = body(browser.call("GET", "/api/v1/users/me/profile", null)).path("data");
        assertThat(reread.path("displayName").asString()).isEqualTo("干饭达人");
        assertThat(reread.path("avatarUrl").asString()).isEqualTo(imageUrl);
        assertThat(response.body()).doesNotContain("passwordHash", "passwordChangedAt", PASSWORD);
    }

    @Test
    void updateTrimsNicknameAndClearsAvatar() throws Exception {
        Browser browser = login(owner);
        var response = browser.call("PUT", "/api/v1/users/me/profile", cleared("  干饭达人  "));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body(response).path("data").path("displayName").asString()).isEqualTo("干饭达人");
        assertThat(body(response).path("data").path("avatarUrl").isNull()).isTrue();
    }

    @Test
    void rejectBlankOrTooLongNickname() throws Exception {
        Browser browser = login(owner);
        assertThat(browser.call("PUT", "/api/v1/users/me/profile", cleared("   ")).statusCode()).isEqualTo(400);
        assertThat(browser.call("PUT", "/api/v1/users/me/profile", cleared("干".repeat(51))).statusCode()).isEqualTo(400);
        assertThat(body(browser.call("GET", "/api/v1/users/me/profile", null)).path("data")
                .path("displayName").asString()).isEqualTo("integration user");
    }

    @Test
    void rejectForeignOrMalformedAvatar() throws Exception {
        Browser browser = login(owner);
        String foreign = body(login(other).upload(png())).path("data").path("imageUrl").asString();
        assertThat(browser.call("PUT", "/api/v1/users/me/profile",
                Map.of("displayName", "干饭达人", "avatarUrl", foreign)).statusCode()).isEqualTo(400);
        assertThat(browser.call("PUT", "/api/v1/users/me/profile",
                Map.of("displayName", "干饭达人", "avatarUrl", "https://evil.example/x.png")).statusCode()).isEqualTo(400);
        assertThat(body(browser.call("GET", "/api/v1/users/me/profile", null)).path("data")
                .path("avatarUrl").isNull()).isTrue();
    }

    @Test
    void disabledUsersCannotReuseTheSessionToUpdateProfile() throws Exception {
        Browser browser = login(owner);
        jdbc.update("UPDATE foodtime.users SET status = 'disabled' WHERE id = ?", owner.id());
        // 写操作沿用登录/登出同一会话失效约定：会话失效后写请求被拒绝（403），读请求返回401
        assertThat(browser.call("PUT", "/api/v1/users/me/profile", cleared("干饭达人")).statusCode()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT display_name FROM foodtime.users WHERE id = ?", String.class, owner.id()))
                .isEqualTo("integration user");
    }

    private byte[] png() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    private static Map<String, Object> cleared(String displayName) {
        var body = new java.util.HashMap<String, Object>();
        body.put("displayName", displayName);
        body.put("avatarUrl", null);
        return body;
    }
}
