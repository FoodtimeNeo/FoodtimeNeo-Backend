package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rating-summary.refresh-enabled=false")
class CatalogSubmissionIT extends IntegrationApiSupport {
    private static final Path STORAGE = Path.of(System.getProperty("java.io.tmpdir"), "submissions-test-" + UUID.randomUUID());
    @DynamicPropertySource static void directory(DynamicPropertyRegistry properties) { properties.add("app.storage.directory", STORAGE::toString); }

    @Test
    void dishSubmissionIsPendingWithNullablePriceAndCannotSpoofOwnership() throws Exception {
        Browser browser = login(owner);
        var response = browser.call("POST", "/api/v1/stalls/" + stall + "/submissions",
                Map.of("name", "new dish", "imageUrl", image(browser), "userId", other.id(), "status", "approved"));
        assertThat(response.statusCode()).isEqualTo(202);
        UUID id = UUID.fromString(body(response).path("data").path("id").asString());
        assertThat(jdbc.queryForObject("SELECT submitted_by_user_id FROM foodtime.dish_submissions WHERE id = ?", UUID.class, id)).isEqualTo(owner.id());
        assertThat(jdbc.queryForObject("SELECT price FROM foodtime.dish_submissions WHERE id = ?", java.math.BigDecimal.class, id)).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.dish_submissions WHERE id = ?", String.class, id)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dishes WHERE stall_id = ?", Long.class, stall)).isEqualTo(1);
    }

    @Test
    void stallsRemainPrivateAndHistoryIsOwnedEvenWhenParentIsDisabled() throws Exception {
        Browser browser = login(owner);
        var created = browser.call("POST", "/api/v1/dining-halls/" + hall + "/submissions", Map.of("name", "new stall", "coverImageUrl", image(browser)));
        assertThat(created.statusCode()).isEqualTo(202);
        jdbc.update("UPDATE foodtime.dining_halls SET status = 'inactive' WHERE id = ?", hall);
        var history = body(browser.call("GET", "/api/v1/users/me/stall-submissions", null)).path("data");
        assertThat(history.path("total").asLong()).isEqualTo(1);
        assertThat(history.path("items").get(0).path("status").asString()).isEqualTo("pending");
        assertThat(body(login(other).call("GET", "/api/v1/users/me/stall-submissions?userId=" + owner.id(), null)).path("data").path("total").asLong()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.stalls WHERE dining_hall_id = ?", Long.class, hall)).isEqualTo(1);
    }

    @Test
    void foreignUploadAndInvalidPriceAndHiddenParentAreRejected() throws Exception {
        String foreign = image(login(other));
        Browser browser = login(owner);
        assertThat(browser.call("POST", "/api/v1/stalls/" + stall + "/submissions", Map.of("name", "foreign", "imageUrl", foreign)).statusCode()).isEqualTo(400);
        String own = image(browser);
        assertThat(browser.call("POST", "/api/v1/stalls/" + stall + "/submissions", Map.of("name", "negative", "imageUrl", own, "price", -1)).statusCode()).isEqualTo(400);
        jdbc.update("UPDATE foodtime.stalls SET status = 'inactive' WHERE id = ?", stall);
        assertThat(browser.call("POST", "/api/v1/stalls/" + stall + "/submissions", Map.of("name", "hidden", "imageUrl", own)).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_submissions WHERE stall_id = ?", Long.class, stall)).isZero();
    }

    private String image(Browser browser) throws Exception {
        var out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", out);
        var upload = browser.upload(out.toByteArray()); assertThat(upload.statusCode()).isEqualTo(201);
        return body(upload).path("data").path("imageUrl").asString();
    }
}
