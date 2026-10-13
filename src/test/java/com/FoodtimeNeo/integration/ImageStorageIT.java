package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rating-summary.refresh-enabled=false")
class ImageStorageIT extends IntegrationApiSupport {
    private static final Path STORAGE = Path.of(System.getProperty("java.io.tmpdir"), "images-test-" + UUID.randomUUID());
    @DynamicPropertySource static void imageDirectory(DynamicPropertyRegistry properties) {
        properties.add("app.storage.directory", () -> STORAGE.toString());
    }

    @Test
    void uploadIsPrivateUntilApprovedAndPublicAccessFollowsParentStatus() throws Exception {
        byte[] bytes = png();
        Browser uploader = login(owner), viewer = login(other);
        var result = uploader.upload(bytes);
        assertThat(result.statusCode()).isEqualTo(201);
        var data = body(result).path("data");
        String reference = data.path("imageUrl").asString();
        assertThat(data.path("width").asInt()).isEqualTo(20);
        assertThat(data.path("height").asInt()).isEqualTo(10);
        assertThat(uploader.image(reference).body()).isEqualTo(bytes);
        assertThat(viewer.image(reference).statusCode()).isEqualTo(404);
        UUID image = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status) VALUES (?, ?, ?, ?, 'pending')", image, dish, owner.id(), reference);
        assertThat(viewer.image(reference).statusCode()).isEqualTo(404);
        jdbc.update("UPDATE foodtime.dish_images SET status = 'approved' WHERE id = ?", image);
        assertThat(viewer.image(reference).statusCode()).isEqualTo(200);
        assertThat(viewer.image(reference).body()).isEqualTo(bytes);
        jdbc.update("UPDATE foodtime.dining_halls SET status = 'inactive' WHERE id = ?", hall);
        assertThat(viewer.image(reference).statusCode()).isEqualTo(404);
        assertThat(uploader.image(reference).statusCode()).isEqualTo(200);
    }

    @Test
    void administratorCanPreviewPendingImagesButAnonymousUsersCannotUpload() throws Exception {
        Browser uploader = login(owner);
        String reference = body(uploader.upload(png())).path("data").path("imageUrl").asString();
        assertThat(login(account("admin")).image(reference).statusCode()).isEqualTo(200);
        Browser anonymous = new Browser(); anonymous.csrf();
        assertThat(anonymous.upload(png()).statusCode()).isEqualTo(401);
    }

    @Test
    void fakeImageContentIsRejectedEvenWhenDeclaredAsPng() throws Exception {
        assertThat(login(owner).upload("not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8)).statusCode()).isEqualTo(400);
    }

    private byte[] png() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
