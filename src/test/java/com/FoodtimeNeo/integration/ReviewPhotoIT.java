package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.moderation.service.ModerationService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="app.ai.triage.review-photo-approve-above=0.99")
class ReviewPhotoIT extends IntegrationApiSupport {
    @Autowired ModerationService moderation;
    @Test void photoIsBoundToOwnedReviewAndVisibleOnlyAfterBothApprovals() throws Exception {
        Browser author=login(owner), viewer=login(other), admin=login(account("admin"));
        var review=author.call("POST",dishPath("/reviews"),Map.of("rating",5,"content","好吃"));
        UUID reviewId=UUID.fromString(body(review).path("data").path("id").asString());
        var bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8,5,BufferedImage.TYPE_INT_RGB),"png",bytes);
        String image=body(author.upload(bytes.toByteArray())).path("data").path("imageUrl").asString();
        var input=Map.of("imageUrl",image,"reviewSubmissionId",reviewId.toString());
        var created=author.call("POST",dishPath("/image-submissions"),input);
        assertThat(created.statusCode()).isEqualTo(201);
        String photoId=body(created).path("data").path("id").asString();
        assertThat(body(author.call("POST",dishPath("/image-submissions"),input)).path("data").path("id").asString()).isEqualTo(photoId);
        String otherImage=body(viewer.upload(bytes.toByteArray())).path("data").path("imageUrl").asString();
        assertThat(viewer.call("POST",dishPath("/image-submissions"),Map.of("imageUrl",otherImage,"reviewSubmissionId",reviewId.toString())).statusCode()).isEqualTo(404);
        assertThat(admin.call("POST","/api/v1/admin/moderation/review/"+reviewId,Map.of("action","approve")).statusCode()).isEqualTo(200);
        assertThat(body(viewer.call("GET",dishPath("/reviews"),null)).path("data").path("items").get(0).path("imageUrl").isNull()).isTrue();
        assertThat(admin.call("POST","/api/v1/admin/moderation/photo/"+photoId,Map.of("action","approve")).statusCode()).isEqualTo(200);
        assertThat(body(viewer.call("GET",dishPath("/reviews"),null)).path("data").path("items").get(0).path("imageUrl").asString()).isEqualTo(image);
        assertThat(body(viewer.call("GET",dishPath("/images"),null)).path("data").path("total").asLong()).isEqualTo(1);
    }

    @Test void highConfidenceMatchingReviewPhotoCanBeApprovedByAi() throws Exception {
        Browser author=login(owner);
        var review=author.call("POST",dishPath("/reviews"),Map.of("rating",4,"content","好吃"));
        String reviewId=body(review).path("data").path("id").asString();
        var bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8,5,BufferedImage.TYPE_INT_RGB),"png",bytes);
        String image=body(author.upload(bytes.toByteArray())).path("data").path("imageUrl").asString();
        var photo=author.call("POST",dishPath("/image-submissions"),Map.of("imageUrl",image,"reviewSubmissionId",reviewId));
        UUID photoId=UUID.fromString(body(photo).path("data").path("id").asString());
        moderation.recordReviewPhotoVerdict(photoId,new AiVerdict("LOW",0.99,java.util.List.of(),"匹配","test-vision"));
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.dish_images WHERE id=?",String.class,photoId)).isEqualTo("approved");
        assertThat(jdbc.queryForObject("SELECT actor FROM foodtime.content_moderation_logs WHERE dish_image_id=?",String.class,photoId)).isEqualTo("ai");
        assertThat(body(author.call("GET",dishPath("/images"),null)).path("data").path("total").asLong()).isEqualTo(1);
    }
}
