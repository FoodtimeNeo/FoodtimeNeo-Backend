package com.FoodtimeNeo.integration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityAdminIT extends IntegrationApiSupport {
 @Test void onlyAdministratorsCanReadFullPendingContentAndRemoveWithReason()throws Exception{
  var author=login(owner);var viewer=login(other);var administrator=account("admin");var admin=login(administrator);
  UUID post=UUID.fromString(body(author.call("POST","/api/v1/community/posts",communityPost(author,"人工审核完整配图"))).path("data").path("id").asString());
  String path="/api/v1/admin/community/posts/"+post;
  assertThat(viewer.call("GET",path,null).statusCode()).isEqualTo(403);
  var response=body(admin.call("GET",path,null)).path("data");
  assertThat(response.path("userEmail").asString()).isEqualTo(owner.email());
  var queue=body(admin.call("GET","/api/v1/admin/community/posts?status=pending&size=100",null)).path("data").path("items");
  var queued=java.util.stream.StreamSupport.stream(queue.spliterator(),false).filter(r->r.path("id").asString().equals(post.toString())).findFirst().orElseThrow();
  assertThat(queued.path("userEmail").asString()).isEqualTo(owner.email());
  var detail=response.path("post");
  assertThat(detail.path("title").asString()).isEqualTo("食记");assertThat(detail.path("images").size()).isEqualTo(1);
  assertThat(admin.call("DELETE",path,Map.of("reason"," ")).statusCode()).isEqualTo(400);
  assertThat(admin.call("POST",path,Map.of("action","approve")).statusCode()).isEqualTo(200);
  assertThat(admin.call("DELETE",path,Map.of("reason","违反社区规则")).statusCode()).isEqualTo(200);
  assertThat(viewer.call("GET","/api/v1/community/posts/"+post,null).statusCode()).isEqualTo(404);
  var logs=body(admin.call("GET","/api/v1/admin/moderation/logs?size=100",null)).path("data").path("items");
  var log=java.util.stream.StreamSupport.stream(logs.spliterator(),false).filter(r->r.path("submissionId").asString().equals(post.toString())).findFirst().orElseThrow();
  assertThat(log.path("userEmail").asString()).isEqualTo(owner.email());
  assertThat(log.path("content").asString()).isEqualTo("人工审核完整配图");
  assertThat(log.path("imageUrl").asString()).isNotBlank();
  assertThat(log.path("submittedAt").asString()).isNotBlank();
  assertThat(jdbc.queryForObject("SELECT deleted_by_user_id FROM foodtime.community_posts WHERE id=?",UUID.class,post)).isEqualTo(administrator.id());
  assertThat(jdbc.queryForObject("SELECT delete_reason FROM foodtime.community_posts WHERE id=?",String.class,post)).isEqualTo("违反社区规则");
 }
}
