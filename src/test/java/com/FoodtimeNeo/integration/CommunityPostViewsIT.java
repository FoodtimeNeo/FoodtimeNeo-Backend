package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityPostViewsIT extends IntegrationApiSupport {
    private UUID create(Browser author,boolean approved) throws Exception {
        var response=author.call("POST","/api/v1/community/posts",communityPost(author,"浏览量测试"));
        assertThat(response.statusCode()).isEqualTo(202);
        UUID id=UUID.fromString(body(response).path("data").path("id").asString());
        if(approved) assertThat(login(account("admin")).call("POST","/api/v1/admin/community/posts/"+id,Map.of("action","approve")).statusCode()).isEqualTo(200);
        return id;
    }
    private String path(UUID id) { return "/api/v1/community/posts/"+id; }
    private long count(UUID id) { return jdbc.queryForObject("SELECT view_count FROM foodtime.community_posts WHERE id=?",Long.class,id); }
    @Test void eachOpenCountsOnceWhileReadsAndRetriesDoNot() throws Exception {
        var author=login(owner); var viewer=login(other); UUID id=create(author,true),request=UUID.randomUUID();
        assertThat(body(viewer.call("GET",path(id),null)).path("data").path("viewCount").asLong()).isZero();
        viewer.call("GET","/api/v1/community/feed/cursor",null);
        assertThat(count(id)).isZero();
        for(int i=0;i<2;i++) {
            var viewed=viewer.call("POST",path(id)+"/views",Map.of("clientRequestId",request));
            assertThat(viewed.statusCode()).isEqualTo(200);
            assertThat(body(viewed).path("data").path("viewCount").asLong()).isEqualTo(1);
        }
        assertThat(viewer.call("POST",path(id)+"/views",Map.of("clientRequestId",UUID.randomUUID())).statusCode()).isEqualTo(200);
        assertThat(body(viewer.call("GET",path(id),null)).path("data").path("viewCount").asLong()).isEqualTo(2);
        assertThat(viewer.call("POST",path(id)+"/views",Map.of()).statusCode()).isEqualTo(400);
        assertThat(count(id)).isEqualTo(2);
    }
    @Test void concurrentUniqueOpensAreNotLostAndConcurrentRetriesAreDeduplicated() throws Exception {
        var author=login(owner); var viewer=login(other); UUID id=create(author,true),same=UUID.randomUUID();
        try(var pool=Executors.newFixedThreadPool(6)) {
            List<Callable<Integer>> work=new ArrayList<>();
            for(int i=0;i<8;i++) { UUID request=UUID.randomUUID(); work.add(() -> viewer.call("POST",path(id)+"/views",Map.of("clientRequestId",request)).statusCode()); }
            for(int i=0;i<8;i++) work.add(() -> viewer.call("POST",path(id)+"/views",Map.of("clientRequestId",same)).statusCode());
            for(var result:pool.invokeAll(work)) assertThat(result.get()).isEqualTo(200);
        }
        assertThat(count(id)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM foodtime.community_post_views WHERE post_id=?",Long.class,id)).isEqualTo(9);
    }
    @Test void unavailablePostsAreNotCountedAndOwnerPreviewDoesNotCount() throws Exception {
        var author=login(owner); var viewer=login(other); UUID id=create(author,false);
        var request=Map.of("clientRequestId",UUID.randomUUID());
        assertThat(author.call("POST",path(id)+"/views",request).statusCode()).isEqualTo(200);
        assertThat(viewer.call("POST",path(id)+"/views",request).statusCode()).isEqualTo(404);
        assertThat(login(account("admin")).call("POST","/api/v1/admin/community/posts/"+id,Map.of("action","reject","reason","测试拒绝")).statusCode()).isEqualTo(200);
        assertThat(author.call("POST",path(id)+"/views",request).statusCode()).isEqualTo(200);
        assertThat(viewer.call("POST",path(UUID.randomUUID())+"/views",request).statusCode()).isEqualTo(404);
        assertThat(author.call("DELETE",path(id),null).statusCode()).isEqualTo(200);
        assertThat(author.call("POST",path(id)+"/views",request).statusCode()).isEqualTo(404);
        assertThat(count(id)).isZero();
    }
}
