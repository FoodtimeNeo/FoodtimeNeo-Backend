package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImageStorageIT extends IntegrationApiSupport {
    private static final Path STORAGE = integrationStorageDirectory();

    private void assertSamePixels(byte[] actual, byte[] expected) throws Exception {
        var a=ImageIO.read(new java.io.ByteArrayInputStream(actual));
        var b=ImageIO.read(new java.io.ByteArrayInputStream(expected));
        assertThat(a.getWidth()).isEqualTo(b.getWidth());
        assertThat(a.getHeight()).isEqualTo(b.getHeight());
        assertThat(a.getRGB(0,0,a.getWidth(),a.getHeight(),null,0,a.getWidth()))
                .isEqualTo(b.getRGB(0,0,b.getWidth(),b.getHeight(),null,0,b.getWidth()));
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
        assertSamePixels(uploader.image(reference).body(), bytes);
        assertThat(viewer.image(reference).statusCode()).isEqualTo(404);
        UUID image = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status) VALUES (?, ?, ?, ?, 'pending')", image, dish, owner.id(), reference);
        assertThat(viewer.image(reference).statusCode()).isEqualTo(404);
        jdbc.update("UPDATE foodtime.dish_images SET status = 'approved' WHERE id = ?", image);
        assertThat(viewer.image(reference).statusCode()).isEqualTo(200);
        assertSamePixels(viewer.image(reference).body(), bytes);
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

    @Test
    void truncatedPngIsAClientErrorRatherThanAStorageOutage() throws Exception {
        var response=login(owner).upload(java.util.Arrays.copyOf(png(),16));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(body(response).path("code").asString()).isEqualTo("IMAGE_INVALID");
    }

    @Test
    void approvedImagesAreImmutableWhileStagingPreviewsAreNotStored() throws Exception {
        Browser uploader = login(owner), viewer = login(other);
        String reference = body(uploader.upload(png())).path("data").path("imageUrl").asString();
        var staging = uploader.image(reference);
        assertThat(staging.statusCode()).isEqualTo(200);
        assertThat(staging.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        UUID image = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status) VALUES (?, ?, ?, ?, 'approved')", image, dish, owner.id(), reference);
        var published = viewer.image(reference);
        assertThat(published.statusCode()).isEqualTo(200);
        assertThat(published.headers().firstValue("Cache-Control").orElseThrow()).contains("immutable");
        String etag = published.headers().firstValue("ETag").orElseThrow();
        assertThat(viewer.imageIfNoneMatch(reference, etag).statusCode()).isEqualTo(304);
    }

    @Test
    void thumbnailsServeScaledCopiesAndRejectUnknownSizes() throws Exception {
        Browser uploader = login(owner), viewer = login(other);
        var big = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_RGB);
        var fill = new java.util.Random(42);
        for (int x = 0; x < 1000; x += 10) for (int y = 0; y < 800; y += 10) {
            big.setRGB(x, y, fill.nextInt(0xFFFFFF));
        }
        var out = new ByteArrayOutputStream();
        ImageIO.write(big, "png", out);
        String reference = body(uploader.upload(out.toByteArray())).path("data").path("imageUrl").asString();
        UUID image = UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status) VALUES (?, ?, ?, ?, 'approved')", image, dish, owner.id(), reference);
        var thumb = viewer.image(reference + "?size=thumb");
        assertThat(thumb.statusCode()).isEqualTo(200);
        assertThat(thumb.headers().firstValue("Cache-Control").orElseThrow()).contains("immutable");
        var scaled = ImageIO.read(new java.io.ByteArrayInputStream(thumb.body()));
        assertThat(scaled.getWidth()).isEqualTo(480);
        assertThat(scaled.getHeight()).isEqualTo(384);
        assertThat(thumb.body().length).isLessThan(out.size());
        assertThat(viewer.image(reference + "?size=huge").statusCode()).isEqualTo(400);
        String foreign = body(login(account("user")).upload(png())).path("data").path("imageUrl").asString();
        assertThat(viewer.image(foreign + "?size=thumb").statusCode()).isEqualTo(404);
    }

    private byte[] png() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    @Test
    void additionalStallAlbumImagesFollowActiveParentsAndSeparateVariants() throws Exception {
        Browser uploader=login(owner),viewer=login(other);
        String reference=body(uploader.upload(png())).path("data").path("imageUrl").asString();
        UUID album=UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO foodtime.stall_images(id,stall_id,image_url) VALUES (?,?,?)",album,stall,reference);
            var image=viewer.image(reference);
            assertThat(image.statusCode()).isEqualTo(200);
            assertThat(image.headers().firstValue("Cache-Control").orElseThrow()).contains("immutable");
            String etag=image.headers().firstValue("ETag").orElseThrow();
            assertThat(viewer.imageIfNoneMatch(reference+"?size=thumb",etag).statusCode()).isEqualTo(200);
            jdbc.update("UPDATE foodtime.stalls SET status='inactive' WHERE id=?",stall);
            assertThat(viewer.imageIfNoneMatch(reference,etag).statusCode()).isEqualTo(404);
            assertThat(uploader.image(reference).headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        } finally {jdbc.update("DELETE FROM foodtime.stall_images WHERE id=?",album);}
    }
}
