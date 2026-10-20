package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewReplyIT extends IntegrationApiSupport {
    @Test void pendingReplyIsPrivateIdempotentAndUsesTheHumanQueue() throws Exception {
        UUID root=formal(owner,4);Browser user=login(other), admin=login(account("admin"));
        UUID request=UUID.randomUUID();var payload=Map.of("clientRequestId",request,"content","pending reply");
        var created=user.call("POST",path(root),payload);
        assertThat(created.statusCode()).isEqualTo(202);
        UUID reply=UUID.fromString(body(created).path("data").path("id").asString());
        assertThat(body(user.call("POST",path(root),payload)).path("data").path("id").asString()).isEqualTo(reply.toString());
        assertThat(user.call("POST",path(root),Map.of("clientRequestId",request,"content","changed")).statusCode()).isEqualTo(409);
        assertThat(body(user.call("GET",path(root),null)).path("data").path("total").asLong()).isZero();
        var mine=body(user.call("GET","/api/v1/users/me/review-replies",null)).path("data");
        assertThat(mine.path("items").get(0).path("status").asString()).isEqualTo("pending");
        assertThat(admin.call("GET","/api/v1/admin/moderation?status=pending&size=100",null).body()).contains("dish-review-reply",reply.toString());
        approve(admin,reply);
        assertThat(admin.call("POST","/api/v1/admin/moderation/dish-review-reply/"+reply,Map.of("action","approve")).statusCode()).isEqualTo(409);
        var rows=body(user.call("GET",path(root),null)).path("data");
        assertThat(rows.path("total").asLong()).isEqualTo(1);
        assertThat(rows.path("items").get(0).path("content").asString()).isEqualTo("pending reply");
        assertThat(jdbc.queryForObject("SELECT review_count FROM foodtime.dish_stats WHERE dish_id=?",Long.class,dish)).isEqualTo(1);
    }
    @Test void nestedRepliesStayInOneThreadAndDeletedTargetsUseTombstones() throws Exception {
        UUID root=formal(owner,4),another=formal(other,3);
        Browser a=login(owner),b=login(other),admin=login(account("admin"));
        UUID first=submit(b,root,"secret removed text",null);approve(admin,first);
        UUID child=submit(a,root,"child stays",first);approve(admin,child);
        assertThat(a.call("POST",path(another),Map.of("clientRequestId",UUID.randomUUID(),"replyToId",first,"content","cross thread")).statusCode()).isEqualTo(404);
        assertThat(a.call("DELETE","/api/v1/review-replies/"+first,null).statusCode()).isEqualTo(404);
        assertThat(b.call("DELETE","/api/v1/review-replies/"+first,null).statusCode()).isEqualTo(200);
        assertThat(b.call("DELETE","/api/v1/review-replies/"+first,null).statusCode()).isEqualTo(200);
        var response=a.call("GET",path(root),null);
        assertThat(response.body()).doesNotContain("secret removed text");
        var data=body(response).path("data");assertThat(data.path("total").asLong()).isEqualTo(1);
        assertThat(data.path("items").get(0).path("replyToDeleted").asBoolean()).isTrue();
        assertThat(data.path("items").get(0).path("id").asString()).isEqualTo(child.toString());
    }
    @Test void cursorPaginationAndRootVisibilityDoNotLeakHiddenContent() throws Exception {
        UUID root=formal(owner,4);Browser user=login(other),admin=login(account("admin"));
        UUID first=submit(user,root,"first",null);approve(admin,first);
        UUID second=submit(user,root,"second",null);approve(admin,second);
        var page=body(user.call("GET",path(root)+"?size=1",null)).path("data");
        assertThat(page.path("hasMore").asBoolean()).isTrue();
        var next=body(user.call("GET",path(root)+"?size=1&cursor="+page.path("nextCursor").asString(),null)).path("data");
        assertThat(next.path("items").get(0).path("id").asString()).isNotEqualTo(page.path("items").get(0).path("id").asString());
        assertThat(next.path("hasMore").asBoolean()).isFalse();
        assertThat(user.call("GET",path(root)+"?cursor=invalid",null).statusCode()).isEqualTo(400);
        jdbc.update("UPDATE foodtime.dish_reviews SET deleted_at=now() WHERE id=?",root);
        assertThat(user.call("GET",path(root),null).statusCode()).isEqualTo(404);
        assertThat(user.call("POST",path(root),Map.of("clientRequestId",UUID.randomUUID(),"content","hidden")).statusCode()).isEqualTo(404);
        assertThat(user.call("DELETE","/api/v1/review-replies/"+first,null).statusCode()).isEqualTo(200);
    }
    @Test void concurrentSameRequestCreatesOneReply() throws Exception {
        UUID root=formal(owner,4);Browser a=login(other),b=login(other);
        var payload=Map.of("clientRequestId",UUID.randomUUID(),"content","once");
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            var x=pool.submit(()->{start.await();return a.call("POST",path(root),payload);});
            var y=pool.submit(()->{start.await();return b.call("POST",path(root),payload);});start.countDown();
            var first=x.get(15,TimeUnit.SECONDS);var second=y.get(15,TimeUnit.SECONDS);
            assertThat(first.statusCode()).isEqualTo(202);assertThat(second.statusCode()).isEqualTo(202);
            assertThat(body(first).path("data").path("id").asString()).isEqualTo(body(second).path("data").path("id").asString());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_review_replies WHERE review_id=?",Long.class,root)).isEqualTo(1);
    }
    private String path(UUID root) {return "/api/v1/reviews/"+root+"/replies";}
    private UUID submit(Browser user,UUID root,String content,UUID target) throws Exception {
        var payload=new HashMap<String,Object>();payload.put("clientRequestId",UUID.randomUUID());payload.put("content",content);
        if(target!=null)payload.put("replyToId",target);
        var response=user.call("POST",path(root),payload);assertThat(response.statusCode()).isEqualTo(202);
        return UUID.fromString(body(response).path("data").path("id").asString());
    }
    private void approve(Browser admin,UUID id) throws Exception {
        assertThat(admin.call("POST","/api/v1/admin/moderation/dish-review-reply/"+id,Map.of("action","approve")).statusCode()).isEqualTo(200);
    }
}
