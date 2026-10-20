package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewReplyLikeIT extends IntegrationApiSupport {
    @Autowired PlatformTransactionManager transactions;
    @Test void approvedReplyLikesAreIdempotentPersonalizedAndIndependentOfRootCounters() throws Exception {
        UUID root=formal(owner,4),reply=reply(root,true);Browser user=login(other),author=login(owner);
        long rootVersion=jdbc.queryForObject("SELECT counter_version FROM foodtime.dish_review_stats WHERE review_id=?",Long.class,root);
        String path=path(reply);
        for(int i=0;i<2;i++) {
            var response=user.call("PUT",path,null);assertThat(response.statusCode()).isEqualTo(200);
            assertThat(body(response).path("data").path("likeCount").asLong()).isEqualTo(1);
        }
        var row=body(user.call("GET","/api/v1/reviews/"+root+"/replies",null)).path("data").path("items").get(0);
        assertThat(row.path("likedByMe").asBoolean()).isTrue();assertThat(row.path("likeCount").asLong()).isEqualTo(1);
        assertThat(body(author.call("GET","/api/v1/reviews/"+root+"/replies",null)).path("data").path("items").get(0).path("likedByMe").asBoolean()).isFalse();
        for(int i=0;i<2;i++) assertThat(body(user.call("DELETE",path,null)).path("data").path("likeCount").asLong()).isZero();
        assertThat(jdbc.queryForObject("SELECT counter_version FROM foodtime.dish_review_stats WHERE review_id=?",Long.class,root)).isEqualTo(rootVersion);
        assertThat(jdbc.queryForObject("SELECT rating_sum FROM foodtime.dish_stats WHERE dish_id=?",Long.class,dish)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT review_count FROM foodtime.dish_stats WHERE dish_id=?",Long.class,dish)).isEqualTo(1);
    }
    @Test void hiddenPendingAndDeletedTargetsRejectNewLikesButAllowCancellation() throws Exception {
        UUID root=formal(owner,4),pending=reply(root,false),visible=reply(root,true);Browser user=login(other);
        assertThat(user.call("PUT",path(pending),null).statusCode()).isEqualTo(404);
        assertThat(new Browser().call("PUT",path(visible),null).statusCode()).isIn(401,403);
        assertThat(user.call("PUT",path(visible),null).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE foodtime.dish_review_replies SET deleted_at=now() WHERE id=?",visible);
        assertThat(user.call("PUT",path(visible),null).statusCode()).isEqualTo(404);
        assertThat(user.call("DELETE",path(visible),null).statusCode()).isEqualTo(200);
        UUID second=reply(root,true);user.call("PUT",path(second),null);
        jdbc.update("UPDATE foodtime.dish_reviews SET deleted_at=now() WHERE id=?",root);
        assertThat(user.call("PUT",path(second),null).statusCode()).isEqualTo(404);
        assertThat(user.call("DELETE",path(second),null).statusCode()).isEqualTo(200);
    }
    @Test void concurrentDuplicateLikesCountOnce() throws Exception {
        UUID root=formal(owner,4),reply=reply(root,true);Browser a=login(other),b=login(other);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            var x=pool.submit(()->{start.await();return a.call("PUT",path(reply),null).statusCode();});
            var y=pool.submit(()->{start.await();return b.call("PUT",path(reply),null).statusCode();});start.countDown();
            assertThat(x.get(15,TimeUnit.SECONDS)).isEqualTo(200);assertThat(y.get(15,TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(jdbc.queryForObject("SELECT like_count FROM foodtime.dish_review_reply_stats WHERE reply_id=?",Long.class,reply)).isEqualTo(1);
    }
    @Test void rollbackRestoresLikeDetailAndCounterTogether() {
        UUID root=formal(owner,4),reply=reply(root,true);
        assertThatThrownBy(()->new TransactionTemplate(transactions).executeWithoutResult(status->{
            jdbc.update("INSERT INTO foodtime.dish_review_reply_likes(reply_id,user_id) VALUES (?,?)",reply,other.id());
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT like_count FROM foodtime.dish_review_reply_stats WHERE reply_id=?",Long.class,reply)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.dish_review_reply_likes WHERE reply_id=?",Long.class,reply)).isZero();
    }
    @Test void adminRemovalRequiresRoleAndReasonAndPreservesHistory() throws Exception {
        UUID root=formal(owner,4),reply=reply(root,true);Browser user=login(other),admin=login(account("admin"));
        user.call("PUT",path(reply),null);
        String removal="/api/v1/admin/review-replies/"+reply;
        assertThat(user.call("DELETE",removal,Map.of("reason","bad")).statusCode()).isEqualTo(403);
        assertThat(admin.call("DELETE",removal,Map.of("reason","")).statusCode()).isEqualTo(400);
        assertThat(admin.call("DELETE",removal,Map.of("reason","违规")).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM foodtime.dish_review_replies WHERE id=?",Boolean.class,reply)).isTrue();
        assertThat(jdbc.queryForObject("SELECT reply_count FROM foodtime.dish_review_stats WHERE review_id=?",Long.class,root)).isZero();
        assertThat(user.call("PUT",path(reply),null).statusCode()).isEqualTo(404);
        assertThat(user.call("DELETE",path(reply),null).statusCode()).isEqualTo(200);
    }
    private String path(UUID reply) {return "/api/v1/review-replies/"+reply+"/like";}
    private UUID reply(UUID root,boolean approved) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO foodtime.dish_review_replies(id,review_id,user_id,client_request_id,content) VALUES (?,?,?,?,?)",id,root,owner.id(),UUID.randomUUID(),"test reply");
        if(approved) jdbc.update("UPDATE foodtime.dish_review_replies SET status='approved',reviewed_by_user_id=?,reviewed_at=now() WHERE id=?",owner.id(),id);
        return id;
    }
}
