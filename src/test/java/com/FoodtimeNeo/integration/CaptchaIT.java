package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.auth.captcha.ChallengeProvider;
import com.FoodtimeNeo.auth.captcha.GeneratedChallenge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(CaptchaIT.FixedProvider.class)
class CaptchaIT extends IntegrationApiSupport {
    private static final String PREFIX = "foodtime:integration:captcha:" + UUID.randomUUID();

    @DynamicPropertySource
    static void captchaProperties(DynamicPropertyRegistry properties) {
        properties.add("app.auth.captcha.enabled", () -> true);
        properties.add("app.auth.captcha.redis-namespace", () -> PREFIX);
    }

    @Autowired private StringRedisTemplate redis;

    @AfterEach
    void clearCaptchaKeys() {
        var keys = redis.keys(PREFIX + "*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }

    @Test
    void secondLoginRequestRequiresAOneTimeDeviceBoundChallenge() throws Exception {
        var user = account("user");
        Browser browser = new Browser();
        browser.csrf();
        var login = Map.of("account", user.email(), "password", PASSWORD);
        assertThat(browser.call("POST", "/api/v1/auth/login", login).statusCode()).isEqualTo(200);

        browser.csrf();
        var challenged = browser.call("POST", "/api/v1/auth/login", login);
        assertThat(challenged.statusCode()).isEqualTo(400);
        assertThat(body(challenged).path("code").asString()).isEqualTo("CAPTCHA_REQUIRED");

        var challenge = browser.call("GET", "/api/v1/auth/captcha", null);
        assertThat(challenge.statusCode()).isEqualTo(200);
        assertThat(challenge.headers().firstValue("cache-control").orElse("")).contains("no-store");
        var data = body(challenge).path("data");
        String challengeId = data.path("challengeId").asString();
        assertThat(challengeId).matches("[0-9a-f-]{36}");
        assertThat(data.path("expiresAt").asString()).isNotBlank();
        byte[] png = Base64.getDecoder().decode(data.path("imageBase64").asString());
        assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);

        var wrong = browser.call("POST", "/api/v1/auth/login",
                Map.of("account", user.email(), "password", PASSWORD,
                        "challengeId", challengeId, "challengeAnswer", "8"));
        assertThat(wrong.statusCode()).isEqualTo(400);
        assertThat(body(wrong).path("code").asString()).isEqualTo("CAPTCHA_INVALID");

        // The wrong answer consumed the challenge; the original challenge cannot be replayed.
        var replay = browser.call("POST", "/api/v1/auth/login",
                Map.of("account", user.email(), "password", PASSWORD,
                        "challengeId", challengeId, "challengeAnswer", "7"));
        assertThat(replay.statusCode()).isEqualTo(400);
        assertThat(body(replay).path("code").asString()).isEqualTo("CAPTCHA_INVALID");

        var fresh = browser.call("GET", "/api/v1/auth/captcha", null);
        String freshId = body(fresh).path("data").path("challengeId").asString();
        var solved = browser.call("POST", "/api/v1/auth/login",
                Map.of("account", user.email(), "password", PASSWORD,
                        "challengeId", freshId, "challengeAnswer", "7"));
        assertThat(solved.statusCode()).isEqualTo(200);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedProvider {
        @Bean
        @Primary
        ChallengeProvider fixedChallengeProvider() {
            return () -> new GeneratedChallenge("2 + 5 = ?", 7, png());
        }

        private static byte[] png() {
            try {
                BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(image, "png", output);
                return output.toByteArray();
            } catch (Exception failure) {
                throw new IllegalStateException(failure);
            }
        }
    }
}
