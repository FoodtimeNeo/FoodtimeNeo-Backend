package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.media.service.StagedImageCleanup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.storage.cleanup-max-age=PT0S",
                "app.storage.cleanup-initial-delay=PT30M"})
class StagedImageCleanupIT extends IntegrationApiSupport {
    @Autowired
    private StagedImageCleanup cleanup;

    @Test
    void orphanStagedFilesAreDeletedWhileReferencedOnesAreKept() throws Exception {
        Browser browser = login(owner);
        String orphan = body(browser.upload(png())).path("data").path("imageUrl").asString();
        String kept = body(browser.upload(png())).path("data").path("imageUrl").asString();
        UUID imageId = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status) VALUES (?, ?, ?, ?, 'pending')",
                imageId, dish, owner.id(), kept);
        cleanup.clean();
        assertThat(browser.image(orphan).statusCode()).isEqualTo(404);
        assertThat(browser.image(kept).statusCode()).isEqualTo(200);
    }

    private byte[] png() throws Exception {
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20, 10, java.awt.image.BufferedImage.TYPE_INT_RGB),
                "png", out);
        return out.toByteArray();
    }
}
