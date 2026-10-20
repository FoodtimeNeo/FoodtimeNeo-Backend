package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"app.ai.triage.provider=fake","app.ai.triage.initial-delay=PT30M"})
class ReviewReplyAiIT extends IntegrationApiSupport {
    @DynamicPropertySource static void enable(DynamicPropertyRegistry properties) {properties.add("app.ai.triage.enabled",()->"true");}
    @Test void replyTextUsesTheInstantAiChainAndAuditLog() throws Exception {
        UUID root=formal(owner,4);Browser user=login(other);
        var response=user.call("POST","/api/v1/reviews/"+root+"/replies",Map.of("clientRequestId",UUID.randomUUID(),"content","放心通过这条回复"));
        assertThat(response.statusCode()).isEqualTo(202);
        UUID reply=UUID.fromString(body(response).path("data").path("id").asString());
        String status="pending";
        for(int i=0;i<100&&status.equals("pending");i++) {Thread.sleep(100);status=jdbc.queryForObject("SELECT status FROM foodtime.dish_review_replies WHERE id=?",String.class,reply);}
        assertThat(status).isEqualTo("approved");
        assertThat(jdbc.queryForObject("SELECT reply_count FROM foodtime.dish_review_stats WHERE review_id=?",Long.class,root)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT actor FROM foodtime.content_moderation_logs WHERE dish_review_reply_id=?",String.class,reply)).isEqualTo("ai");
    }
}
