package com.FoodtimeNeo.integration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="app.rating-summary.refresh-enabled=false")
class ImageSubmissionIT extends IntegrationApiSupport {
    private static final Path STORAGE=Path.of(System.getProperty("java.io.tmpdir"),"photo-test-"+UUID.randomUUID());
    @DynamicPropertySource static void directory(DynamicPropertyRegistry p) { p.add("app.storage.directory",STORAGE::toString); }
    @Test
    void pendingImageIsOwnedPrivateAndNeverPublishedByClientStatus() throws Exception {
        Browser author=login(owner), viewer=login(other); String image=image(author);
        var created=author.call("POST",dishPath("/image-submissions"),Map.of("imageUrl",image,"status","approved","uploadedByUserId",other.id()));
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(body(created).path("data").path("status").asString()).isEqualTo("pending");
        assertThat(body(author.call("GET","/api/v1/users/me/image-submissions",null)).path("data").path("total").asLong()).isEqualTo(1);
        assertThat(body(viewer.call("GET","/api/v1/users/me/image-submissions",null)).path("data").path("total").asLong()).isZero();
        assertThat(viewer.image(image).statusCode()).isEqualTo(404);
        assertThat(body(viewer.call("GET",dishPath("/images"),null)).path("data").path("total").asLong()).isZero();
        assertThat(viewer.call("POST",dishPath("/image-submissions"),Map.of("imageUrl",image)).statusCode()).isEqualTo(400);
    }
    @Test
    void rejectionWritesReasonAndExactlyOneImageAuditLink() throws Exception {
        Browser author=login(owner), admin=login(account("admin"));
        UUID id=submit(author,image(author));
        assertThat(author.call("POST",path(id),Map.of("action","approve")).statusCode()).isEqualTo(403);
        assertThat(admin.call("POST",path(id),Map.of("action","reject","reason"," ")).statusCode()).isEqualTo(400);
        assertThat(admin.call("POST",path(id),Map.of("action","reject","reason","照片模糊")).statusCode()).isEqualTo(200);
        var item=body(author.call("GET","/api/v1/users/me/image-submissions",null)).path("data").path("items").get(0);
        assertThat(item.path("reviewReason").asString()).isEqualTo("照片模糊");
        assertThat(item.path("reviewedAt").asString()).isNotEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.content_moderation_logs WHERE dish_image_id=? AND num_nonnulls(review_submission_id,dish_submission_id,stall_submission_id)=0",Long.class,id)).isEqualTo(1);
        assertThat(admin.call("POST",path(id),Map.of("action","approve")).statusCode()).isEqualTo(409);
    }
    @Test
    void concurrentApprovalMakesOneImageAndLogAndRespectsParentVisibility() throws Exception {
        Browser author=login(owner), viewer=login(other); String image=image(author); UUID id=submit(author,image);
        Account actor=account("admin"); Browser first=login(actor),second=login(actor);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            Future<Integer> a=pool.submit(() -> {start.await();return first.call("POST",path(id),Map.of("action","approve")).statusCode();});
            Future<Integer> b=pool.submit(() -> {start.await();return second.call("POST",path(id),Map.of("action","approve")).statusCode();});
            start.countDown(); assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.content_moderation_logs WHERE dish_image_id=?",Long.class,id)).isEqualTo(1);
        assertThat(body(viewer.call("GET",dishPath("/images"),null)).path("data").path("total").asLong()).isEqualTo(1);
        assertThat(viewer.image(image).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE foodtime.dining_halls SET status='inactive' WHERE id=?",hall);
        assertThat(viewer.image(image).statusCode()).isEqualTo(404);
        assertThat(body(author.call("GET","/api/v1/users/me/image-submissions",null)).path("data").path("items").get(0).path("dishName").asString()).isEqualTo("integration dish");
    }
    @Test
    void inactiveTargetCannotAcceptOrApproveAnImage() throws Exception {
        Browser author=login(owner),admin=login(account("admin")); String image=image(author); UUID id=submit(author,image);
        jdbc.update("UPDATE foodtime.stalls SET status='inactive' WHERE id=?",stall);
        assertThat(author.call("POST",dishPath("/image-submissions"),Map.of("imageUrl",image)).statusCode()).isEqualTo(404);
        assertThat(admin.call("POST",path(id),Map.of("action","approve")).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.dish_images WHERE id=?",String.class,id)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.content_moderation_logs WHERE dish_image_id=?",Long.class,id)).isZero();
    }
    private UUID submit(Browser browser,String image) throws Exception {
        var response=browser.call("POST",dishPath("/image-submissions"),Map.of("imageUrl",image));
        assertThat(response.statusCode()).isEqualTo(201); return UUID.fromString(body(response).path("data").path("id").asString());
    }
    private String image(Browser browser) throws Exception {
        var bytes=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(8,5,BufferedImage.TYPE_INT_RGB),"png",bytes);
        var response=browser.upload(bytes.toByteArray()); assertThat(response.statusCode()).isEqualTo(201);
        return body(response).path("data").path("imageUrl").asString();
    }
    private String path(UUID id) { return "/api/v1/admin/moderation/photo/"+id; }
}
