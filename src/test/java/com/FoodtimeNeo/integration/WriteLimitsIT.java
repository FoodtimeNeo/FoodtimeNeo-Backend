package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.content.submissions.user-limit=2",
                "app.content.uploads.user-limit=2"})
class WriteLimitsIT extends IntegrationApiSupport {
    @Test
    void reviewSubmissionsShareOneThrottle() throws Exception {
        Browser browser = login(owner);
        assertThat(submitReview(browser, UUID.randomUUID()).statusCode()).isEqualTo(202);
        assertThat(submitReview(browser, UUID.randomUUID()).statusCode()).isEqualTo(202);
        var limited = submitReview(browser, UUID.randomUUID());
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.body()).contains("SUBMISSION_RATE_LIMITED");
        assertThat(limited.headers().firstValue("Retry-After")).isPresent();
    }

    @Test
    void catalogSubmissionsDrawFromTheSameBucket() throws Exception {
        Browser browser = login(owner);
        String imageUrl = body(browser.upload(png())).path("data").path("imageUrl").asString();
        assertThat(submitReview(browser, UUID.randomUUID()).statusCode()).isEqualTo(202);
        assertThat(submitReview(browser, UUID.randomUUID()).statusCode()).isEqualTo(202);
        var limited = browser.call("POST", "/api/v1/stalls/" + stall + "/submissions",
                Map.of("name", "extra dish", "price", 9.9, "imageUrl", imageUrl));
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.body()).contains("SUBMISSION_RATE_LIMITED");
    }

    @Test
    void uploadsHaveTheirOwnThrottle() throws Exception {
        Browser browser = login(owner);
        byte[] image = png();
        assertThat(browser.upload(image).statusCode()).isEqualTo(201);
        assertThat(browser.upload(image).statusCode()).isEqualTo(201);
        var limited = browser.upload(image);
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.body()).contains("UPLOAD_RATE_LIMITED");
        assertThat(limited.headers().firstValue("Retry-After")).isPresent();
    }

    private java.net.http.HttpResponse<String> submitReview(Browser browser, UUID dishId) throws Exception {
        jdbc.update("INSERT INTO foodtime.dishes(id,stall_id,name,price) VALUES (?, ?, 'limit dish', 5.00)", dishId, stall);
        return browser.call("POST", "/api/v1/dishes/" + dishId + "/reviews",
                Map.of("rating", 5, "content", "limit test review"));
    }

    private byte[] png() throws Exception {
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20, 10, java.awt.image.BufferedImage.TYPE_INT_RGB),
                "png", out);
        return out.toByteArray();
    }
}
