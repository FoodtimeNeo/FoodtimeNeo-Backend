package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.feedback.FeedbackCsv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeedbackIT extends IntegrationApiSupport {
    private static final String OWN="/api/v1/users/me/feedback",ADMIN="/api/v1/admin/feedback";
    @AfterEach void cleanupFeedback() {
        for(UUID user:users)jdbc.update("DELETE FROM foodtime.user_feedback WHERE user_id=?",user);
    }
    private String create(Browser actor,String content)throws Exception {
        var response=actor.call("POST",OWN,Map.of("type","function","content",content,"clientRequestId",UUID.randomUUID()));
        assertThat(response.statusCode()).isEqualTo(200);
        return body(response).path("data").path("id").asString();
    }
    private String quote(String value) { return "\""+value.replace("\"","\"\"")+"\""; }
    private String csv(String... rows) { return "\uFEFF反馈编号,反馈类型,反馈内容,回复,处理状态\r\n"+String.join("\r\n",rows)+"\r\n"; }
    private String line(String id,String content,String reply,String status) {
        return String.join(",",quote(id),quote("功能反馈"),quote(content),quote(reply),quote(status));
    }
    @Test void ownerIsolationAndSuperadminOnlyManagement()throws Exception {
        Browser author=login(owner),outsider=login(other),admin=login(account("admin")),superadmin=login(account("superadmin"));
        String id=create(author,"需要改进");
        assertThat(new Browser().call("GET",OWN,null).statusCode()).isIn(401,403);
        assertThat(outsider.call("GET",OWN+"/"+id,null).statusCode()).isEqualTo(404);
        assertThat(outsider.call("GET",OWN+"/"+id+"/replies",null).statusCode()).isEqualTo(404);
        assertThat(body(outsider.call("GET",OWN,null)).path("data").path("items")).isEmpty();
        for(Browser denied:List.of(author,admin)) {
            assertThat(denied.call("GET",ADMIN,null).statusCode()).isEqualTo(403);
            assertThat(denied.call("POST",ADMIN+"/export",null).statusCode()).isEqualTo(403);
            assertThat(denied.call("POST",ADMIN+"/import",Map.of("csv",csv(line(id,"需要改进","越权回复","已完成")))).statusCode()).isEqualTo(403);
            assertThat(denied.call("PUT",ADMIN+"/"+id,Map.of("reply","越权回复","clientRequestId",UUID.randomUUID())).statusCode()).isEqualTo(403);
        }
        assertThat(superadmin.call("GET",ADMIN+"/"+id,null).statusCode()).isEqualTo(200);
    }
    @Test void retriedSubmissionUsesSameIdAndRejectsReusedIdForDifferentContent()throws Exception {
        Browser author=login(owner);UUID request=UUID.randomUUID();
        var payload=Map.of("type","usage","content","相同请求","clientRequestId",request);
        String id=body(author.call("POST",OWN,payload)).path("data").path("id").asString();
        assertThat(body(author.call("POST",OWN,payload)).path("data").path("id").asString()).isEqualTo(id);
        assertThat(author.call("POST",OWN,Map.of("type","usage","content","改动内容","clientRequestId",request)).statusCode()).isEqualTo(409);
        assertThat(body(author.call("GET",OWN,null)).path("data").path("total").asLong()).isEqualTo(1);
    }
    @Test void exportStartsProcessingAndMapsIdenticalContentsByIdWithFullReplyHistory()throws Exception {
        Browser author=login(owner),second=login(other),admin=login(account("superadmin"));
        String content="同样的建议，包含,逗号与\"引号\"\n第二行😊";
        String first=create(author,content),last=create(second,content);
        var exported=body(admin.call("POST",ADMIN+"/export",null)).path("data");
        assertThat(exported.path("count").asInt()).isEqualTo(2);
        var parsed=FeedbackCsv.parse(exported.path("csv").asString());assertThat(parsed).hasSize(3);
        assertThat(body(author.call("GET",OWN+"/"+first,null)).path("data").path("status").asString()).isEqualTo("processing");
        var result=body(admin.call("POST",ADMIN+"/import",Map.of("csv",csv(line(first,content,"回复第一位","处理中"),line(last,content,"回复第二位","已完成"))))).path("data");
        assertThat(result.path("updated").asInt()).isEqualTo(2);
        assertThat(body(author.call("GET",OWN+"/"+first+"/replies",null)).path("data").path("items").get(0).path("content").asString()).isEqualTo("回复第一位");
        assertThat(body(second.call("GET",OWN+"/"+last+"/replies",null)).path("data").path("items").get(0).path("content").asString()).isEqualTo("回复第二位");
        admin.call("POST",ADMIN+"/import",Map.of("csv",csv(line(first,content,"再次回复","处理中"))));
        assertThat(body(author.call("GET",OWN+"/"+first+"/replies",null)).path("data").path("total").asInt()).isEqualTo(2);
        assertThat(body(admin.call("POST",ADMIN+"/export",null)).path("data").path("count").asInt()).isEqualTo(1);
        assertThat(body(admin.call("GET",ADMIN+"?archived=true",null)).path("data").path("items")).hasSize(1);
    }
    @Test void partialImportReportsUnknownModifiedInvalidDuplicateAndCompletedRows()throws Exception {
        Browser author=login(owner),admin=login(account("superadmin"));
        String valid=create(author,"有效"),changed=create(author,"原内容"),invalid=create(author,"状态"),done=create(author,"完成");
        admin.call("POST",ADMIN+"/export",null);
        admin.call("PUT",ADMIN+"/"+done,Map.of("reply","完成回复","status","completed","clientRequestId",UUID.randomUUID()));
        String sheet=csv(line(valid,"有效","正确回复","处理中"),line(changed,"改过内容","不应写入","已完成"),
                line(invalid,"状态","不应写入","错误状态"),line(UUID.randomUUID().toString(),"不存在","不应写入","已完成"),
                line(done,"完成","旧表覆盖","已完成"),line(valid,"有效","重复编号","已完成"));
        var result=body(admin.call("POST",ADMIN+"/import",Map.of("csv",sheet))).path("data");
        assertThat(result.path("updated").asInt()).isEqualTo(1);assertThat(result.path("skipped").asInt()).isEqualTo(5);
        assertThat(result.path("issues")).hasSize(5);
        assertThat(body(author.call("GET",OWN+"/"+changed+"/replies",null)).path("data").path("items")).isEmpty();
        assertThat(body(author.call("GET",OWN+"/"+done+"/replies",null)).path("data").path("total").asInt()).isEqualTo(1);
    }
    @Test void repeatedImportAndBlankCellsNeverDuplicateOrEraseReplies()throws Exception {
        Browser author=login(owner),admin=login(account("superadmin"));String id=create(author,"保留历史");
        String sheet=csv(line(id,"保留历史","第一次回复",""));
        admin.call("POST",ADMIN+"/import",Map.of("csv",sheet));
        assertThat(body(admin.call("POST",ADMIN+"/import",Map.of("csv",sheet))).path("data").path("updated").asInt()).isZero();
        admin.call("POST",ADMIN+"/import",Map.of("csv",csv(line(id,"保留历史","",""))));
        assertThat(body(author.call("GET",OWN+"/"+id+"/replies",null)).path("data").path("total").asInt()).isEqualTo(1);
    }
    @Test void formulaLikeContentRoundTripsAsSafeSpreadsheetText()throws Exception {
        Browser author=login(owner),admin=login(account("superadmin"));String content="=HYPERLINK(\"https://example.test\")";
        String id=create(author,content);
        String exported=body(admin.call("POST",ADMIN+"/export",null)).path("data").path("csv").asString();
        assertThat(FeedbackCsv.parse(exported).get(1).get(2)).isEqualTo("'"+content);
        assertThat(body(admin.call("POST",ADMIN+"/import",Map.of("csv",csv(line(id,"'"+content,"已处理","已完成"))))).path("data").path("updated").asInt()).isEqualTo(1);
    }
    @Test void visitorQuotaCountsSuccessfulRecordsAndNotRequestRetries()throws Exception {
        Browser visitor=login(account("visitor"));UUID request=UUID.randomUUID();
        var input=Map.of("type","usage","content","访客反馈","clientRequestId",request);
        assertThat(visitor.call("POST",OWN,input).statusCode()).isEqualTo(200);
        assertThat(visitor.call("POST",OWN,input).statusCode()).isEqualTo(200);
        create(visitor,"第二条");
        assertThat(visitor.call("POST",OWN,Map.of("type","usage","content","第三条","clientRequestId",UUID.randomUUID())).statusCode()).isEqualTo(429);
        assertThat(visitor.call("POST",OWN,input).statusCode()).isEqualTo(200);
    }
    @Test void concurrentVisitorSubmissionsCannotExceedDailyAccountQuota()throws Exception {
        Account account=account("visitor");Browser first=login(account),second=login(account),third=login(account);
        ExecutorService workers=Executors.newFixedThreadPool(3);
        try {
            var barrier=new CyclicBarrier(3);var tasks=new ArrayList<Future<Integer>>();
            for(Browser browser:List.of(first,second,third))tasks.add(workers.submit(()->{
                barrier.await(10,TimeUnit.SECONDS);
                return browser.call("POST",OWN,Map.of("type","usage","content","并发反馈","clientRequestId",UUID.randomUUID())).statusCode();
            }));
            var statuses=new ArrayList<Integer>();for(var task:tasks)statuses.add(task.get(20,TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(200,200,429);
        }finally{workers.shutdownNow();}
    }
    @Test void certifiedUserGetsFivePerDayAndOldDayRecordsDoNotConsumeToday()throws Exception {
        Browser author=login(owner);
        for(int i=0;i<5;i++)create(author,"认证用户反馈 "+i);
        assertThat(author.call("POST",OWN,Map.of("type","usage","content","第六条","clientRequestId",UUID.randomUUID())).statusCode()).isEqualTo(429);
        jdbc.update("UPDATE foodtime.user_feedback SET created_at=now()-interval '1 day' WHERE user_id=?",owner.id());
        create(author,"新的一天");
    }
    @Test void deviceQuotaIsSharedAcrossAccountsAndIndependentOfLoginSession()throws Exception {
        Browser browser=new Browser();
        for(int i=0;i<5;i++) {
            Account visitor=account("visitor");browser.csrf();
            assertThat(browser.call("POST","/api/v1/auth/login",Map.of("account",visitor.email(),"password",PASSWORD)).statusCode()).isEqualTo(200);browser.csrf();
            create(browser,"设备反馈一");create(browser,"设备反馈二");
            assertThat(browser.call("POST","/api/v1/auth/logout",null).statusCode()).isEqualTo(200);
        }
        Account next=account("user");browser.csrf();
        assertThat(browser.call("POST","/api/v1/auth/login",Map.of("account",next.email(),"password",PASSWORD)).statusCode()).isEqualTo(200);browser.csrf();
        var denied=browser.call("POST",OWN,Map.of("type","usage","content","设备第十一条","clientRequestId",UUID.randomUUID()));
        assertThat(denied.statusCode()).isEqualTo(429);
        assertThat(body(denied).path("code").asString()).isEqualTo("FEEDBACK_DEVICE_DAILY_LIMIT");
    }
    @Test void concurrentRepeatedImportsAppendExactlyOneReply()throws Exception {
        Browser author=login(owner),admin=login(account("superadmin"));String id=create(author,"并发导入");
        String sheet=csv(line(id,"并发导入","只追加一次",""));ExecutorService workers=Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier barrier=new CyclicBarrier(2);var tasks=new ArrayList<Future<Integer>>();
            for(int i=0;i<2;i++)tasks.add(workers.submit(()->{barrier.await(10,TimeUnit.SECONDS);return body(admin.call("POST",ADMIN+"/import",Map.of("csv",sheet))).path("data").path("updated").asInt();}));
            int updated=0;for(var task:tasks)updated+=task.get(20,TimeUnit.SECONDS);assertThat(updated).isEqualTo(1);
            assertThat(body(author.call("GET",OWN+"/"+id+"/replies",null)).path("data").path("total").asInt()).isEqualTo(1);
        }finally{workers.shutdownNow();}
    }
    @Test void manualCompletionCanRetryWithoutDuplicateReplyOrReopeningArchive()throws Exception {
        Browser author=login(owner),admin=login(account("superadmin"));String id=create(author,"完成重试");
        var input=Map.of("reply","已完成回复","status","completed","clientRequestId",UUID.randomUUID());
        assertThat(admin.call("PUT",ADMIN+"/"+id,input).statusCode()).isEqualTo(200);
        assertThat(admin.call("PUT",ADMIN+"/"+id,input).statusCode()).isEqualTo(200);
        assertThat(body(author.call("GET",OWN+"/"+id+"/replies",null)).path("data").path("total").asInt()).isEqualTo(1);
        assertThat(body(author.call("GET",OWN+"?archived=true",null)).path("data").path("items")).hasSize(1);
    }
}
