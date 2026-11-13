package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.service.FakeAiProvider;
import com.FoodtimeNeo.moderation.service.ModerationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Holds a real asynchronous diagnosis across an HTTP edit, then releases its obsolete result. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"app.ai.triage.provider=fake","app.ai.triage.initial-delay=PT30M"})
class CommunityPostEditAiIT extends IntegrationApiSupport {
    @DynamicPropertySource static void enableAi(DynamicPropertyRegistry properties){properties.add("app.ai.triage.enabled",()->"true");}
    @MockitoSpyBean FakeAiProvider provider;
    @MockitoSpyBean ModerationService moderation;
    @Test void oldNetworkDiagnosisCannotApproveAnEditedPostOrBlockItsSubmission()throws Exception{
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var low=new AiVerdict("LOW",0.99,List.of(),"旧正文通过","fake");
        doAnswer(call->{entered.countDown();assertThat(release.await(15,TimeUnit.SECONDS)).isTrue();return low;})
            .when(provider).checkText(anyString(),contains("旧阻塞正文"));
        doReturn(new AiVerdict("LOW",0.8,List.of(),"新正文须人工核验","fake"))
            .when(provider).checkText(anyString(),contains("新待审正文"));
        doReturn(low).when(provider).checkImage(anyString(),any(),anyString());
        var author=login(owner);var original=communityPost(author,"旧阻塞正文");
        var created=author.call("POST","/api/v1/community/posts",original);assertThat(created.statusCode()).isEqualTo(202);
        UUID id=UUID.fromString(body(created).path("data").path("id").asString());
        try {
            assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
            var input=new LinkedHashMap<>(original);input.put("expectedRevision",1);input.put("clientRequestId",UUID.randomUUID());input.put("content","新待审正文");
            try(var pool=Executors.newSingleThreadExecutor()){
                assertThat(pool.submit(()->author.call("PUT","/api/v1/community/posts/"+id,input)).get(5,TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            }
            verify(moderation,timeout(5000)).recordPostVerdict(eq(id),eq(2L),any());
            release.countDown();verify(moderation,timeout(5000)).recordPostVerdict(eq(id),eq(1L),any());
            assertThat(jdbc.queryForObject("SELECT status FROM foodtime.community_posts WHERE id=?",String.class,id)).isEqualTo("pending");
            assertThat(jdbc.queryForObject("SELECT content_revision FROM foodtime.community_posts WHERE id=?",Long.class,id)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT reason FROM foodtime.ai_moderation_results WHERE submission_type='community-post' AND submission_id=?",String.class,id)).isEqualTo("新正文须人工核验");
        }finally{release.countDown();}
    }
}
