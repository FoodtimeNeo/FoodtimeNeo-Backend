package com.FoodtimeNeo.integration;
import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.service.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.moderation.service.ModerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
 properties={"app.ai.triage.provider=fake","app.ai.triage.initial-delay=PT30M"})
class CommunityAiFallbackIT extends IntegrationApiSupport {
 @DynamicPropertySource static void enabled(DynamicPropertyRegistry r){r.add("app.ai.triage.enabled",()->"true");}
 @MockitoSpyBean FakeAiProvider provider;
 @Autowired ModerationService moderation;
 @Test void providerFailureRoutesToHumanInsteadOfLeavingContentStuck()throws Exception{
  doThrow(new AiUnavailableException("模拟服务中断")).when(provider).checkText(anyString(),contains("故障测试"));
  var author=login(owner);UUID id=UUID.fromString(body(author.call("POST","/api/v1/community/posts",communityPost(author,"故障测试"))).path("data").path("id").asString());
  awaitManual(id);
  assertThat(jdbc.queryForObject("SELECT manual_review_reason FROM foodtime.community_posts WHERE id=?",String.class,id)).isEqualTo("ai_unavailable");
  assertThat(login(other).call("GET","/api/v1/community/posts/"+id,null).statusCode()).isEqualTo(404);
 }
 @Test void uncertainImageNeedsHumanEvenWithConfidentText()throws Exception{
  var author=login(owner);UUID id=UUID.fromString(body(author.call("POST","/api/v1/community/posts",communityPost(author,"放心，配图结论仍需人工确认"))).path("data").path("id").asString());
  awaitManual(id);
  assertThat(jdbc.queryForObject("SELECT status FROM foodtime.community_posts WHERE id=?",String.class,id)).isEqualTo("pending");
  assertThat(jdbc.queryForObject("SELECT manual_review_reason FROM foodtime.community_posts WHERE id=?",String.class,id)).isEqualTo("uncertain");
  assertThatThrownBy(()->moderation.autoDecide("community-post",id,new AiVerdict("LOW",0.99,List.of(),"迟到的自动结论","fake"),true)).isInstanceOf(BusinessException.class).hasMessageContaining("人工");
 }
 private void awaitManual(UUID id)throws Exception{
  String stage="";for(int i=0;i<60&&!stage.equals("manual_pending");i++){Thread.sleep(100);stage=jdbc.queryForObject("SELECT review_stage FROM foodtime.community_posts WHERE id=?",String.class,id);}
  assertThat(stage).isEqualTo("manual_pending");
 }
}
