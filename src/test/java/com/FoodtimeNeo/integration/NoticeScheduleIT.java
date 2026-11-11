package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoticeScheduleIT extends IntegrationApiSupport {
    private static final String ADMIN="/api/v1/admin/notices",OWN="/api/v1/users/me/notices";
    private final Instant start=Instant.parse("2026-11-11T07:30:00Z");
    @MockitoBean private Clock clock;
    @BeforeEach void time(){when(clock.instant()).thenReturn(start);when(clock.getZone()).thenReturn(ZoneOffset.UTC);}
    @AfterEach void cleanup(){
        jdbc.update("UPDATE foodtime.config SET notice_id=NULL WHERE id=1");
        for(UUID user:users)jdbc.update("DELETE FROM foodtime.notices WHERE created_by=?",user);
    }
    private Map<String,Object> schedule(Instant publication,Instant display,Instant end,boolean update,boolean published,boolean enabled) {
        Map<String,Object> result=new HashMap<>();result.put("publishedAt",publication);result.put("displayStartsAt",display);
        result.put("displayEndsAt",end);result.put("isUpdate",update);result.put("published",published);result.put("enabled",enabled);return result;
    }
    private String create(Browser admin,String title,Map<String,Object> schedule)throws Exception {
        var response=admin.call("POST",ADMIN+"/markdown",Map.of("content","# "+title,"schedule",schedule));
        assertThat(response.statusCode()).isEqualTo(201);
        return body(response).path("data").path("id").asString();
    }
    private List<String> ids(tools.jackson.databind.JsonNode rows){var ids=new ArrayList<String>();for(var row:rows)ids.add(row.path("id").asString());return ids;}
    @Test void publicationDisplayAndExpiryHaveSeparatePreciseBoundaries()throws Exception {
        Browser admin=login(account("superadmin")),user=login(owner);
        Instant publication=start.plusSeconds(60),display=publication.plusSeconds(60),end=display.plusSeconds(60);
        String id=create(admin,"定时公告",schedule(publication,display,end,false,true,true));
        assertThat(body(user.call("GET",OWN,null)).path("data").path("total").asInt()).isZero();
        assertThat(body(user.call("GET",OWN+"/unread-count",null)).path("data").path("unreadCount").asInt()).isZero();
        assertThat(body(user.call("GET",OWN+"/feed",null)).path("data").path("items")).isEmpty();
        assertThat(user.call("POST",OWN+"/"+id+"/confirm",Map.of("dontShowAgain",false)).statusCode()).isEqualTo(404);
        when(clock.instant()).thenReturn(publication);
        assertThat(body(user.call("GET",OWN,null)).path("data").path("total").asInt()).isEqualTo(1);
        assertThat(body(user.call("GET",OWN+"/current-set",null)).path("data")).isEmpty();
        when(clock.instant()).thenReturn(display);
        assertThat(ids(body(user.call("GET",OWN+"/current-set",null)).path("data"))).containsExactly(id);
        when(clock.instant()).thenReturn(end);
        assertThat(body(user.call("GET",OWN+"/current-set",null)).path("data")).isEmpty();
        assertThat(body(user.call("GET",OWN,null)).path("data").path("total").asInt()).isEqualTo(1);
        assertThat(user.call("POST",OWN+"/"+id+"/read",Map.of()).statusCode()).isEqualTo(200);
    }
    @Test void newestPerCategoryIsOrderedUpdateFirstAndSuppressionDoesNotFallBackToOlderNotice()throws Exception {
        Browser admin=login(account("superadmin")),user=login(owner);
        create(admin,"旧更新",schedule(start.minusSeconds(300),null,null,true,true,true));
        create(admin,"旧普通",schedule(start.minusSeconds(240),null,null,false,true,true));
        String update=create(admin,"新更新",schedule(start.minusSeconds(180),null,null,true,true,true));
        String normal=create(admin,"新普通",schedule(start.minusSeconds(120),null,null,false,true,true));
        assertThat(ids(body(user.call("GET",OWN+"/current-set",null)).path("data"))).containsExactly(update,normal);
        assertThat(body(user.call("GET",OWN+"/current",null)).path("data").path("id").asString()).isEqualTo(update);
        user.call("POST",OWN+"/"+update+"/confirm",Map.of("dontShowAgain",true));
        assertThat(ids(body(user.call("GET",OWN+"/current-set",null)).path("data"))).containsExactly(normal);
    }
    @Test void draftIsPrivateAndEnablingIsIndependentForEachNotice()throws Exception {
        Browser admin=login(account("superadmin")),user=login(owner);
        String draft=create(admin,"草稿",schedule(start,null,null,false,false,false));
        assertThat(body(user.call("GET",OWN,null)).path("data").path("total").asInt()).isZero();
        assertThat(body(admin.call("GET",ADMIN,null)).path("data").path("total").asInt()).isEqualTo(1);
        assertThat(admin.call("PUT",ADMIN+"/"+draft+"/schedule",schedule(start,null,null,false,true,true)).statusCode()).isEqualTo(200);
        assertThat(ids(body(user.call("GET",OWN+"/current-set",null)).path("data"))).containsExactly(draft);
        admin.call("PUT",ADMIN+"/"+draft+"/schedule",schedule(start,null,null,false,true,false));
        assertThat(body(user.call("GET",OWN+"/current-set",null)).path("data")).isEmpty();
        assertThat(body(user.call("GET",OWN,null)).path("data").path("total").asInt()).isEqualTo(1);
    }
    @Test void dateOnlyDeadlineExpiresAtNextUtcPlusEightMidnight()throws Exception {
        Browser admin=login(account("superadmin")),user=login(owner);var schedule=schedule(start,null,null,false,true,true);
        schedule.put("expiresOn","2026-11-11");String id=create(admin,"今天优惠",schedule);
        var item=body(admin.call("GET",ADMIN+"/"+id,null)).path("data");
        assertThat(item.path("displayEndsAt").asString()).isEqualTo("2026-11-11T16:00:00Z");
        assertThat(item.path("expiresOn").asString()).isEqualTo("2026-11-11");
        when(clock.instant()).thenReturn(Instant.parse("2026-11-11T15:59:59.999Z"));
        assertThat(ids(body(user.call("GET",OWN+"/current-set",null)).path("data"))).containsExactly(id);
        when(clock.instant()).thenReturn(Instant.parse("2026-11-11T16:00:00Z"));
        assertThat(body(user.call("GET",OWN+"/current-set",null)).path("data")).isEmpty();
    }
    @Test void futureNoticeCannotBeSkippedByALaterImmediateNoticeCursor()throws Exception {
        Browser admin=login(account("superadmin")),user=login(owner);
        String future=create(admin,"先创建后发布",schedule(start.plusSeconds(3600),null,null,false,true,true));
        String immediate=create(admin,"后创建立即发布",schedule(start,null,null,false,true,true));
        var before=body(user.call("GET",OWN+"/feed",null)).path("data").path("items");
        assertThat(ids(before)).containsExactly(immediate);long cursor=before.get(0).path("seq").asLong();
        when(clock.instant()).thenReturn(start.plusSeconds(3600));
        var after=body(user.call("GET",OWN+"/feed?since_seq="+cursor,null)).path("data").path("items");
        assertThat(ids(after)).containsExactly(future);assertThat(after.get(0).path("seq").asLong()).isGreaterThan(cursor);
    }
    @Test void concurrentOpeningAllocatesExactlyOnePublicCursor()throws Exception {
        Browser admin=login(account("superadmin")),user=login(owner);
        String future=create(admin,"并发到期",schedule(start.plusSeconds(60),null,null,false,true,true));
        when(clock.instant()).thenReturn(start.plusSeconds(60));ExecutorService workers=Executors.newFixedThreadPool(4);
        try {
            var tasks=new ArrayList<Future<Long>>();var barrier=new CyclicBarrier(4);
            for(int i=0;i<4;i++)tasks.add(workers.submit(()->{barrier.await(10,TimeUnit.SECONDS);return body(user.call("GET",OWN+"/feed",null)).path("data").path("items").get(0).path("seq").asLong();}));
            Set<Long> cursors=new HashSet<>();for(var task:tasks)cursors.add(task.get(20,TimeUnit.SECONDS));assertThat(cursors).hasSize(1);
            assertThat(jdbc.queryForObject("SELECT seq FROM foodtime.notices WHERE notice_id=?",Long.class,UUID.fromString(future))).isIn(cursors);
        }finally{workers.shutdownNow();}
    }
    @Test void invalidScheduleRollsBackContentAndNaiveTimestampsAreRejected()throws Exception {
        Browser admin=login(account("superadmin"));
        var invalid=schedule(start,start.minusSeconds(60),null,false,true,true);
        assertThat(admin.call("POST",ADMIN+"/markdown",Map.of("content","# 无效","schedule",invalid)).statusCode()).isEqualTo(400);
        var naive=schedule(start,null,null,false,true,true);naive.put("publishedAt","2026-11-11T15:30");
        assertThat(admin.call("POST",ADMIN+"/markdown",Map.of("content","# 无时区","schedule",naive)).statusCode()).isEqualTo(400);
        assertThat(body(admin.call("GET",ADMIN,null)).path("data").path("total").asInt()).isZero();
    }
}
