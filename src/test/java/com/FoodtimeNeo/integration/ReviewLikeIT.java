package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewLikeIT extends IntegrationApiSupport {
    @Test void likesAreIdempotentPersonalizedAndSurviveReload() throws Exception {
        UUID review=formal(owner,4);
        Browser user=login(other), author=login(owner);
        String path="/api/v1/reviews/"+review+"/like";
        for(int i=0;i<2;i++) {
            var response=user.call("PUT",path,null);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(body(response).path("data").path("likeCount").asLong()).isEqualTo(1);
        }
        var row=body(user.call("GET",dishPath("/reviews"),null)).path("data").path("items").get(0);
        assertThat(row.path("id").asString()).isEqualTo(review.toString());
        assertThat(row.path("likedByMe").asBoolean()).isTrue();
        assertThat(row.path("likeCount").asLong()).isEqualTo(1);
        assertThat(body(author.call("GET",dishPath("/reviews"),null)).path("data").path("items").get(0).path("likedByMe").asBoolean()).isFalse();
        for(int i=0;i<2;i++) assertThat(body(user.call("DELETE",path,null)).path("data").path("likeCount").asLong()).isZero();
        assertThat(jdbc.queryForObject("SELECT review_count FROM foodtime.dish_stats WHERE dish_id=?",Long.class,dish)).isEqualTo(1);
    }
    @Test void hiddenTargetsRejectNewLikesButAllowCancellation() throws Exception {
        UUID review=formal(owner,4); Browser user=login(other);
        String path="/api/v1/reviews/"+review+"/like";
        user.call("PUT",path,null);
        jdbc.update("UPDATE foodtime.dish_reviews SET deleted_at=now() WHERE id=?",review);
        assertThat(user.call("PUT",path,null).statusCode()).isEqualTo(404);
        assertThat(user.call("DELETE",path,null).statusCode()).isEqualTo(200);
        assertThat(new Browser().call("PUT",path,null).statusCode()).isIn(401,403);
    }
    @Test void concurrentDuplicateLikesCountOnce() throws Exception {
        UUID review=formal(owner,4); Browser a=login(other), b=login(other);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            Callable<Integer> first=() -> {start.await();return a.call("PUT","/api/v1/reviews/"+review+"/like",null).statusCode();};
            Callable<Integer> second=() -> {start.await();return b.call("PUT","/api/v1/reviews/"+review+"/like",null).statusCode();};
            var x=pool.submit(first);var y=pool.submit(second);start.countDown();
            assertThat(x.get(15,TimeUnit.SECONDS)).isEqualTo(200);assertThat(y.get(15,TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(jdbc.queryForObject("SELECT like_count FROM foodtime.dish_review_stats WHERE review_id=?",Long.class,review)).isEqualTo(1);
    }
}
