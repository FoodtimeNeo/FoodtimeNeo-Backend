package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityPinIT extends IntegrationApiSupport {
    private static final String ADMIN="/api/v1/admin/community/pins",VISIBLE="/api/v1/community/pins",FEED="/api/v1/community/feed/cursor";
    private UUID post(String title) {
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO foodtime.community_posts(id,user_id,title,content,status,review_stage,reviewed_by_user_id,reviewed_at) VALUES(?,?,?,?,'approved','completed',?,now())",id,owner.id(),title,title,other.id());return id;
    }
    private UUID pendingPost(String title) {
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO foodtime.community_posts(id,user_id,title,content,status,review_stage,manual_review_reason) VALUES(?,?,?,?,'pending','manual_pending','ai_disabled')",id,owner.id(),title,title);return id;
    }
    private long revision(Browser browser)throws Exception{return body(browser.call("GET",VISIBLE,null)).path("data").path("revision").asLong();}
    private void pin(Browser admin,UUID id)throws Exception{var result=admin.call("PUT",ADMIN+"/"+id,Map.of("expectedRevision",revision(admin)));assertThat(result.statusCode()).as(result.body()).isEqualTo(200);}
    private List<String> ids(tools.jackson.databind.JsonNode values){var ids=new ArrayList<String>();for(var value:values)ids.add(value.path("id").asString());return ids;}
    @Test void onlySuperadminCanManagePinsAndAnonymousCannotReadThem()throws Exception{
        UUID id=post("权限边界");
        for(String role:List.of("visitor","user","admin")) {
            var browser=login(account(role));assertThat(browser.call("GET",VISIBLE,null).statusCode()).isEqualTo(200);
            assertThat(body(browser.call("GET",VISIBLE,null)).path("data").path("canManage").asBoolean()).isFalse();
            assertThat(browser.call("GET",ADMIN,null).statusCode()).isEqualTo(403);
            assertThat(browser.call("PUT",ADMIN+"/"+id,Map.of("expectedRevision",revision(browser))).statusCode()).isEqualTo(403);
        }
        assertThat(new Browser().call("GET",VISIBLE,null).statusCode()).isEqualTo(401);
        var admin=login(account("superadmin"));pin(admin,id);
        assertThat(body(admin.call("GET",VISIBLE,null)).path("data").path("canManage").asBoolean()).isTrue();
    }
    @Test void limitOrderConflictsAndUnpinningPreserveChronology()throws Exception{
        var admin=login(account("superadmin"));UUID a=post("甲"),b=post("乙"),c=post("丙"),d=post("丁");
        var created=jdbc.queryForObject("SELECT created_at FROM foodtime.community_posts WHERE id=?",java.sql.Timestamp.class,a);
        pin(admin,a);pin(admin,b);pin(admin,c);long rev=revision(admin);
        assertThat(admin.call("PUT",ADMIN+"/"+d,Map.of("expectedRevision",rev)).statusCode()).isEqualTo(409);
        assertThat(admin.call("PUT",ADMIN+"/order",Map.of("expectedRevision",rev,"postIds",List.of(c,a,b))).statusCode()).isEqualTo(200);
        assertThat(ids(body(admin.call("GET",VISIBLE,null)).path("data").path("items"))).containsExactly(c.toString(),a.toString(),b.toString());
        assertThat(admin.call("DELETE",ADMIN+"/"+a+"?expectedRevision="+rev,null).statusCode()).isEqualTo(409);
        assertThat(admin.call("DELETE",ADMIN+"/"+a+"?expectedRevision="+revision(admin),null).statusCode()).isEqualTo(200);
        pin(admin,d);
        assertThat(jdbc.queryForObject("SELECT created_at FROM foodtime.community_posts WHERE id=?",java.sql.Timestamp.class,a)).isEqualTo(created);
    }
    @Test void discoveryExcludesPinsWhileSearchFollowingAuthorAndFavoritesKeepThem()throws Exception{
        var admin=login(account("superadmin"));var viewer=login(other);UUID id=post("检索置顶"),regular=post("普通帖子");pin(admin,id);
        var page=body(viewer.call("GET",FEED,null)).path("data");
        assertThat(ids(page.path("items"))).contains(regular.toString()).doesNotContain(id.toString());
        assertThat(ids(page.path("pins").path("items"))).containsExactly(id.toString());assertThat(page.path("total").asLong()).isEqualTo(1);
        assertThat(ids(body(viewer.call("GET",FEED+"?q="+java.net.URLEncoder.encode("检索置顶",java.nio.charset.StandardCharsets.UTF_8),null)).path("data").path("items"))).containsExactly(id.toString());
        assertThat(viewer.call("PUT","/api/v1/users/"+owner.id()+"/follow",null).statusCode()).isEqualTo(200);
        assertThat(ids(body(viewer.call("GET",FEED+"?following=true",null)).path("data").path("items"))).contains(id.toString());
        assertThat(ids(body(viewer.call("GET","/api/v1/community/authors/"+owner.id()+"/posts",null)).path("data").path("items"))).contains(id.toString());
        assertThat(viewer.call("PUT","/api/v1/community/posts/"+id+"/favorite",null).statusCode()).isEqualTo(200);
        assertThat(ids(body(viewer.call("GET","/api/v1/users/me/community-favorites",null)).path("data").path("items"))).contains(id.toString());
    }
    @Test void visibilityChangesKeepTheSlotAndAutomaticallyRestoreTheOriginalOrder()throws Exception{
        var admin=login(account("superadmin"));UUID a=pendingPost("保留的置顶名额"),b=post("后置顶");
        // This reserved state is produced by the separately delivered edit workflow.
        jdbc.update("INSERT INTO foodtime.community_post_pins(post_id,position) VALUES(?,1)",a);pin(admin,b);
        long rev=revision(admin);
        assertThat(ids(body(admin.call("GET",VISIBLE,null)).path("data").path("items"))).containsExactly(b.toString());
        assertThat(ids(body(admin.call("GET",ADMIN,null)).path("data").path("items"))).containsExactly(a.toString(),b.toString());
        jdbc.update("UPDATE foodtime.community_posts SET status='approved',review_stage='completed',reviewed_by_user_id=?,reviewed_at=now() WHERE id=?",other.id(),a);
        assertThat(revision(admin)).isGreaterThan(rev);
        assertThat(ids(body(admin.call("GET",VISIBLE,null)).path("data").path("items"))).containsExactly(a.toString(),b.toString());
        UUID rejected=pendingPost("被拒仍占位");jdbc.update("INSERT INTO foodtime.community_post_pins(post_id,position) VALUES(?,3)",rejected);
        jdbc.update("UPDATE foodtime.community_posts SET status='rejected',review_stage='completed',review_reason='请更正',reviewed_by_user_id=?,reviewed_at=now() WHERE id=?",other.id(),rejected);
        assertThat(body(admin.call("GET",ADMIN,null)).path("data").path("items")).hasSize(3);
        assertThat(ids(body(admin.call("GET",VISIBLE,null)).path("data").path("items"))).doesNotContain(rejected.toString());
        assertThat(admin.call("PUT",ADMIN+"/"+post("名额已满"),Map.of("expectedRevision",revision(admin))).statusCode()).isEqualTo(409);
    }
    @Test void deletedPostsReleaseTheirSlotWhileTheOriginalPostRecordIsRetained()throws Exception{
        var admin=login(account("superadmin"));UUID a=post("移除"),b=post("保留");pin(admin,a);pin(admin,b);
        assertThat(login(owner).call("DELETE","/api/v1/community/posts/"+a,null).statusCode()).isEqualTo(200);
        var rows=body(admin.call("GET",ADMIN,null)).path("data").path("items");assertThat(ids(rows)).containsExactly(b.toString());assertThat(rows.get(0).path("position").asInt()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM foodtime.community_posts WHERE id=?",Boolean.class,a)).isTrue();
        assertThat(ids(body(admin.call("GET",VISIBLE,null)).path("data").path("items"))).containsExactly(b.toString());
    }
    @Test void changedPinsForceAPaginationRestartWithoutSilentlySkippingAnUnpinnedPost()throws Exception{
        var admin=login(account("superadmin"));UUID a=post("旧帖子");post("新帖子");
        var first=body(admin.call("GET",FEED+"?size=1",null)).path("data");String cursor=first.path("nextCursor").asString();long rev=first.path("pins").path("revision").asLong();pin(admin,a);
        var changed=admin.call("GET",FEED+"?size=1&cursor="+cursor+"&pinRevision="+rev,null);
        assertThat(changed.statusCode()).isEqualTo(409);assertThat(body(changed).path("code").asString()).isEqualTo("PIN_SET_CHANGED");
    }
    @Test void invalidOrderingAndUnpublishedTargetsCannotChangeTheList()throws Exception{
        var admin=login(account("superadmin"));UUID a=post("有效"),b=pendingPost("待审核");pin(admin,a);
        assertThat(admin.call("PUT",ADMIN+"/"+b,Map.of("expectedRevision",revision(admin))).statusCode()).isEqualTo(404);
        assertThat(admin.call("PUT",ADMIN+"/order",Map.of("expectedRevision",revision(admin),"postIds",List.of(a,a))).statusCode()).isEqualTo(400);
        assertThat(admin.call("PUT",ADMIN+"/"+b,Map.of()).statusCode()).isEqualTo(400);
        assertThat(ids(body(admin.call("GET",ADMIN,null)).path("data").path("items"))).containsExactly(a.toString());
    }
    @Test void simultaneousAttemptsCannotOccupyMoreThanThreeSlots()throws Exception{
        var first=login(account("superadmin"));var second=login(account("superadmin"));pin(first,post("一"));pin(first,post("二"));
        UUID a=post("竞争甲"),b=post("竞争乙");long rev=revision(first);var barrier=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var x=pool.submit(()->{barrier.await();return first.call("PUT",ADMIN+"/"+a,Map.of("expectedRevision",rev)).statusCode();});
            var y=pool.submit(()->{barrier.await();return second.call("PUT",ADMIN+"/"+b,Map.of("expectedRevision",rev)).statusCode();});
            assertThat(List.of(x.get(15,TimeUnit.SECONDS),y.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(body(first.call("GET",ADMIN,null)).path("data").path("items")).hasSize(3);
    }
}
