package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.service.FakeAiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewPhotoAiIT extends IntegrationApiSupport {
    @DynamicPropertySource static void aiProperties(DynamicPropertyRegistry registry) {
        registry.add("app.ai.triage.enabled",()->"true");
        registry.add("app.ai.triage.provider",()->"fake");
        registry.add("app.ai.single.enabled",()->"false");
        registry.add("app.ai.triage.review-photo-approve-above",()->"0.99");
        registry.add("app.ai.triage.initial-delay",()->"PT30M");
    }
    @MockitoSpyBean FakeAiProvider provider;

    @Test void submissionUsesDishSpecificVisionAndApprovesOnlyMatchingImage() throws Exception {
        doReturn(new AiVerdict("LOW",0.99,List.of(),"匹配","test-vision"))
                .when(provider).checkDishImage(anyString(),any(),anyString(),eq("integration dish"));
        Browser author=login(owner);
        var review=author.call("POST",dishPath("/reviews"),Map.of("rating",4,"content","好吃"));
        String reviewId=body(review).path("data").path("id").asString();
        var bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8,5,BufferedImage.TYPE_INT_RGB),"png",bytes);
        String image=body(author.upload(bytes.toByteArray())).path("data").path("imageUrl").asString();
        var response=author.call("POST",dishPath("/image-submissions"),Map.of("imageUrl",image,"reviewSubmissionId",reviewId));
        UUID id=UUID.fromString(body(response).path("data").path("id").asString());
        String status="pending";
        for(int i=0;i<100 && status.equals("pending");i++){
            Thread.sleep(70);
            status=jdbc.queryForObject("SELECT status FROM foodtime.dish_images WHERE id=?",String.class,id);
        }
        assertThat(status).isEqualTo("approved");
        verify(provider,atLeastOnce()).checkDishImage(anyString(),any(),anyString(),eq("integration dish"));
    }
}
