package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.service.*;
import com.FoodtimeNeo.moderation.mapper.ModerationStore;
import com.FoodtimeNeo.moderation.service.ModerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"app.ai.triage.provider=fake","app.ai.triage.initial-delay=PT30M","app.ai.triage.interval=PT30M"})
class CatalogAiIT extends IntegrationApiSupport {
    private static final Path STORAGE=Path.of(System.getProperty("java.io.tmpdir"),"catalog-ai-"+UUID.randomUUID());
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){
        r.add("app.storage.directory",STORAGE::toString);r.add("app.ai.triage.enabled",()->"true");
        r.add("app.ai.single.enabled",()->"false");r.add("app.ai.triage.provider",()->"fake");
    }
    @MockitoSpyBean FakeAiProvider provider;
    @MockitoSpyBean ModerationStore store;
    @Autowired AiTriageService triage;
    @Autowired ModerationService moderation;
    @Autowired @org.springframework.beans.factory.annotation.Qualifier("aiTriageExecutor")
    java.util.concurrent.Executor executor;
    private AiVerdict verdict(String risk,double confidence,String model){return new AiVerdict(risk,confidence,List.of("测试结论"),"本地测试建议",model);}
    private void answer(String risk,double confidence){
        doReturn(verdict(risk,confidence,"text-test")).when(provider).checkText(anyString(),anyString());
        doReturn(verdict(risk,confidence,"vision-test")).when(provider).checkImage(anyString(),any(),anyString());
    }
    private String image(Browser user)throws Exception{
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(20,10,BufferedImage.TYPE_INT_RGB),"png",out);
        var response=user.upload(out.toByteArray());assertThat(response.statusCode()).isEqualTo(201);return body(response).path("data").path("imageUrl").asString();
    }
    private UUID dish(Browser user,String image)throws Exception{
        var r=user.call("POST","/api/v1/stalls/"+stall+"/submissions",Map.of("name","AI测试菜品","description","菜品说明","imageUrl",image,"price",new BigDecimal("8.20")));
        assertThat(r.statusCode()).isEqualTo(202);return UUID.fromString(body(r).path("data").path("id").asString());
    }
    private UUID photo(Browser user,String image)throws Exception{
        var r=user.call("POST",dishPath("/image-submissions"),Map.of("imageUrl",image));assertThat(r.statusCode()).isEqualTo(201);return UUID.fromString(body(r).path("data").path("id").asString());
    }
    private String table(String type){return type.equals("dish")?"foodtime.dish_submissions":"foodtime.dish_images";}
    private void awaitVerdict(String type,UUID id)throws Exception{
        for(int i=0;i<100;i++){if(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.ai_moderation_results WHERE submission_type=? AND submission_id=?",Long.class,type,id)>0)return;Thread.sleep(70);}
        fail("AI verdict missing for "+type+" "+id);
    }
    private void status(String type,UUID id,String expected){assertThat(jdbc.queryForObject("SELECT status FROM "+table(type)+" WHERE id=?",String.class,id)).isEqualTo(expected);}
    private long logs(UUID id){return jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.content_moderation_logs WHERE dish_submission_id=? OR dish_image_id=?",Long.class,id,id);}
    private UUID seeded(String type){
        UUID id=UUID.randomUUID();String image="/api/v1/media/"+UUID.randomUUID();
        if(type.equals("dish"))jdbc.update("INSERT INTO foodtime.dish_submissions(id,stall_id,submitted_by_user_id,name,description,image_url,price) VALUES (?,?,?,'补扫菜品','说明',?,8.20)",id,stall,owner.id(),image);
        else jdbc.update("INSERT INTO foodtime.dish_images(id,dish_id,uploaded_by_user_id,image_url,status) VALUES (?,?,?,?,'pending')",id,dish,owner.id(),image);
        return id;
    }

    @Test void confidentCleanDishStaysPendingUntilHumanApproves()throws Exception{
        answer("LOW",1);Browser user=login(owner);String image=image(user);UUID id=dish(user,image);awaitVerdict("dish",id);
        status("dish",id,"pending");assertThat(logs(id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dishes WHERE stall_id=?",Long.class,stall)).isEqualTo(1);
        Browser admin=login(account("admin"));var queue=body(admin.call("GET","/api/v1/admin/moderation?type=dish&status=pending&size=100",null)).path("data").path("items");
        var row=java.util.stream.StreamSupport.stream(queue.spliterator(),false).filter(r->r.path("id").asString().equals(id.toString())).findFirst().orElseThrow();
        assertThat(row.path("aiRiskLevel").asString()).isEqualTo("LOW");assertThat(row.path("aiReason").asString()).contains("文字","图片");
        assertThat(admin.call("POST","/api/v1/admin/moderation/dish/"+id,Map.of("action","approve","price",0)).statusCode()).isEqualTo(200);
        status("dish",id,"approved");assertThat(jdbc.queryForObject("SELECT actor FROM foodtime.content_moderation_logs WHERE dish_submission_id=?",String.class,id)).isEqualTo("human");
        assertThat(jdbc.queryForObject("SELECT reviewed_by_ai_model FROM foodtime.dish_submissions WHERE id=?",String.class,id)).isNull();
    }
    @Test void confidentCleanPhotoAlsoNeedsHumanApproval()throws Exception{
        answer("LOW",1);Browser user=login(owner);UUID id=photo(user,image(user));awaitVerdict("photo",id);status("photo",id,"pending");assertThat(logs(id)).isZero();
        assertThat(login(account("admin")).call("POST","/api/v1/admin/moderation/photo/"+id,Map.of("action","approve")).statusCode()).isEqualTo(200);
        status("photo",id,"approved");assertThat(jdbc.queryForObject("SELECT actor FROM foodtime.content_moderation_logs WHERE dish_image_id=?",String.class,id)).isEqualTo("human");
    }
    @Test void unsafeDishTextIsRejectedWithoutPublishingAndPreservesSubmittedPrice()throws Exception{
        answer("HIGH",.99);Browser user=login(owner);UUID id=dish(user,image(user));awaitVerdict("dish",id);status("dish",id,"rejected");
        assertThat(jdbc.queryForObject("SELECT price FROM foodtime.dish_submissions WHERE id=?",BigDecimal.class,id)).isEqualByComparingTo("8.20");
        assertThat(jdbc.queryForObject("SELECT reviewed_by_ai_model FROM foodtime.dish_submissions WHERE id=?",String.class,id)).isEqualTo("text-test");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dishes WHERE stall_id=?",Long.class,stall)).isEqualTo(1);assertThat(logs(id)).isEqualTo(1);
        verify(provider,never()).checkImage(anyString(),any(),anyString());
    }
    @Test void unsafeImageRejectsBothNewDishAndStandalonePhoto()throws Exception{
        doReturn(verdict("LOW",1,"text-test")).when(provider).checkText(anyString(),anyString());
        doReturn(verdict("HIGH",.99,"vision-test")).when(provider).checkImage(anyString(),any(),anyString());
        Browser user=login(owner);String image=image(user);UUID d=dish(user,image),p=photo(user,image);awaitVerdict("dish",d);awaitVerdict("photo",p);
        status("dish",d,"rejected");status("photo",p,"rejected");assertThat(logs(d)).isEqualTo(1);assertThat(logs(p)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT reviewed_by_ai_model FROM foodtime.dish_images WHERE id=?",String.class,p)).isEqualTo("vision-test");
        assertThat(jdbc.queryForObject("SELECT actor FROM foodtime.content_moderation_logs WHERE dish_image_id=?",String.class,p)).isEqualTo("ai");
    }
    @Test void uncertaintyAndLowConfidenceHighRiskStayHuman()throws Exception{
        Browser user=login(owner);String image=image(user);
        for(String risk:List.of("MEDIUM","HIGH")){
            answer(risk,risk.equals("HIGH")?.5:.99);UUID d=dish(user,image),p=photo(user,image);awaitVerdict("dish",d);awaitVerdict("photo",p);
            status("dish",d,"pending");status("photo",p,"pending");assertThat(logs(d)).isZero();assertThat(logs(p)).isZero();
        }
    }
    @Test void modelOutageKeepsPendingAndSweepCanRetry()throws Exception{
        CountDownLatch called=new CountDownLatch(2);
        var unavailable=new java.util.concurrent.atomic.AtomicBoolean(true);
        doReturn(verdict("LOW",1,"text-test")).when(provider).checkText(anyString(),anyString());
        doAnswer(invocation->{
            if(unavailable.get()) {called.countDown();throw new AiUnavailableException("本地测试故障");}
            return verdict("LOW",1,"vision-test");
        }).when(provider).checkImage(anyString(),any(),anyString());
        Browser user=login(owner);String image=image(user);UUID d=dish(user,image),p=photo(user,image);assertThat(called.await(8,TimeUnit.SECONDS)).isTrue();
        // A provider invocation is not the end of its asynchronous task. Do not reset/re-stub a running spy.
        var pool=((org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor)executor).getThreadPoolExecutor();
        for(int i=0;i<100&&(pool.getActiveCount()>0||!pool.getQueue().isEmpty());i++)Thread.sleep(20);
        assertThat(pool.getActiveCount()).isZero();assertThat(pool.getQueue()).isEmpty();
        status("dish",d,"pending");status("photo",p,"pending");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.ai_moderation_results WHERE submission_id IN (?,?)",Long.class,d,p)).isZero();
        unavailable.set(false);triage.triageOnce();awaitVerdict("dish",d);awaitVerdict("photo",p);status("dish",d,"pending");status("photo",p,"pending");
    }
    @Test void missingFileIsAdviceNotAutomaticRejection(){
        UUID d=seeded("dish"),p=seeded("photo");triage.triageOnce();status("dish",d,"pending");status("photo",p,"pending");
        assertThat(jdbc.queryForObject("SELECT model FROM foodtime.ai_moderation_results WHERE submission_type='dish' AND submission_id=?",String.class,d)).isEqualTo("system");
        assertThat(logs(d)).isZero();assertThat(logs(p)).isZero();
    }
    @Test void previouslyRecordedHighRiskAdviceIsRejectedOnceWithoutRediagnosis(){
        for(String type:List.of("dish","photo")){
            UUID id=seeded(type);
            jdbc.update("INSERT INTO foodtime.ai_moderation_results(id,submission_type,submission_id,risk_level,confidence,labels,reason,model) VALUES (?,?,?,'HIGH',.99,'{}','原有高风险建议','old-vision')",UUID.randomUUID(),type,id);
            triage.triageOnce();status(type,id,"rejected");assertThat(logs(id)).isEqualTo(1);triage.triageOnce();assertThat(logs(id)).isEqualTo(1);
        }
    }
    @Test void databaseAlsoRejectsAiAttributedApproval(){
        UUID d=seeded("dish"),p=seeded("photo");
        assertThatThrownBy(()->jdbc.update("UPDATE foodtime.dish_submissions SET status='approved',approved_dish_id=?,reviewed_at=now(),reviewed_by_ai_model='forbidden' WHERE id=?",dish,d)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("UPDATE foodtime.dish_images SET status='approved',reviewed_at=now(),reviewed_by_ai_model='forbidden' WHERE id=?",p)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        status("dish",d,"pending");status("photo",p,"pending");
    }
    @Test void concurrentWorkersCommitOnlyOneVerdictAndOneRejection()throws Exception{
        UUID id=seeded("dish");CountDownLatch start=new CountDownLatch(1);var high=verdict("HIGH",.99,"concurrent-test");
        try(var pool=Executors.newFixedThreadPool(2)){
            Future<Integer> first=pool.submit(()->{start.await();return moderation.recordCatalogVerdict("dish",id,high);});
            Future<Integer> second=pool.submit(()->{start.await();return moderation.recordCatalogVerdict("dish",id,high);});start.countDown();
            assertThat(List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(1,0);
        }
        status("dish",id,"rejected");assertThat(logs(id)).isEqualTo(1);
    }
    @Test void humanDecisionWinsOverLateAiVerdict()throws Exception{
        Browser user=login(owner);String image=image(user);UUID id=seeded("dish");jdbc.update("UPDATE foodtime.dish_submissions SET image_url=? WHERE id=?",image,id);
        assertThat(login(account("admin")).call("POST","/api/v1/admin/moderation/dish/"+id,Map.of("action","approve")).statusCode()).isEqualTo(200);
        assertThat(moderation.recordCatalogVerdict("dish",id,verdict("HIGH",1,"late-test"))).isZero();status("dish",id,"approved");
        assertThat(jdbc.queryForObject("SELECT actor FROM foodtime.content_moderation_logs WHERE dish_submission_id=?",String.class,id)).isEqualTo("human");
    }
    @Test void failedAuditLogRollsBackVerdictAndRejectionTogether(){
        UUID id=seeded("dish");
        doThrow(new IllegalStateException("本地日志故障")).when(store).log(any(),isNull(),eq("ai"),anyString(),eq("reject"),anyString());
        assertThatThrownBy(()->moderation.recordCatalogVerdict("dish",id,verdict("HIGH",1,"rollback-test"))).isInstanceOf(IllegalStateException.class);
        status("dish",id,"pending");assertThat(logs(id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.ai_moderation_results WHERE submission_id=?",Long.class,id)).isZero();
    }
}
