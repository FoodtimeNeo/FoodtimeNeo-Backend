package com.FoodtimeNeo.integration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityAdminIT extends IntegrationApiSupport {
 @ParameterizedTest @ValueSource(strings={"admin","superadmin"})
 void onlyAdministratorsCanReadFullPendingContentAndRemoveWithoutAReason(String role)throws Exception{
  var author=login(owner);var viewer=login(other);var administrator=account(role);var admin=login(administrator);
  UUID post=UUID.fromString(body(author.call("POST","/api/v1/community/posts",communityPost(author,"人工审核完整配图"))).path("data").path("id").asString());
  String path="/api/v1/admin/community/posts/"+post;
  assertThat(viewer.call("GET",path,null).statusCode()).isEqualTo(403);
  assertThat(viewer.call("DELETE",path,Map.of("reason","reason")).statusCode()).isEqualTo(403);
  assertThat(admin.call("DELETE",path,Map.of("reason","x".repeat(501))).statusCode()).isEqualTo(400);
  var response=body(admin.call("GET",path,null)).path("data");
  assertThat(response.path("userEmail").asString()).isEqualTo(owner.email());
  var queue=body(admin.call("GET","/api/v1/admin/community/posts?status=pending&size=100",null)).path("data").path("items");
  var queued=java.util.stream.StreamSupport.stream(queue.spliterator(),false).filter(r->r.path("id").asString().equals(post.toString())).findFirst().orElseThrow();
  assertThat(queued.path("userEmail").asString()).isEqualTo(owner.email());
  var detail=response.path("post");
  assertThat(detail.path("title").asString()).isEqualTo("食记");assertThat(detail.path("images").size()).isEqualTo(1);
  assertThat(admin.call("POST",path,Map.of("action","approve")).statusCode()).isEqualTo(200);
  assertThat(admin.call("DELETE",path,null).statusCode()).isEqualTo(200);
  assertThat(viewer.call("GET","/api/v1/community/posts/"+post,null).statusCode()).isEqualTo(404);
  assertThat(admin.call("DELETE",path,Map.of("reason","reason")).statusCode()).isEqualTo(404);
  var logs=body(admin.call("GET","/api/v1/admin/moderation/logs?size=100",null)).path("data").path("items");
  var log=java.util.stream.StreamSupport.stream(logs.spliterator(),false).filter(r->r.path("submissionId").asString().equals(post.toString())).findFirst().orElseThrow();
  assertThat(log.path("userEmail").asString()).isEqualTo(owner.email());
  assertThat(log.path("content").asString()).isEqualTo("人工审核完整配图");
  assertThat(log.path("imageUrl").asString()).isNotBlank();
  assertThat(log.path("submittedAt").asString()).isNotBlank();
  assertThat(jdbc.queryForObject("SELECT deleted_by_user_id FROM foodtime.community_posts WHERE id=?",UUID.class,post)).isEqualTo(administrator.id());
  assertThat(jdbc.queryForObject("SELECT delete_reason FROM foodtime.community_posts WHERE id=?",String.class,post)).isNull();
 }

 @ParameterizedTest @ValueSource(strings={"admin","superadmin"})
 void administratorsCanDeleteRootCommentsAndRepliesWithoutLosingTheRemainingThread(String role)throws Exception{
  var author=login(owner);var viewer=login(other);var administrator=account(role);var admin=login(administrator);
  UUID post=UUID.fromString(body(author.call("POST","/api/v1/community/posts",communityPost(author,"评论管理"))).path("data").path("id").asString());
  assertThat(admin.call("POST","/api/v1/admin/community/posts/"+post,Map.of("action","approve")).statusCode()).isEqualTo(200);
  UUID root=comment(author,post,null);
  assertThat(admin.call("POST","/api/v1/admin/community/comments/"+root,Map.of("action","approve")).statusCode()).isEqualTo(200);
  UUID reply=comment(viewer,post,root);
  assertThat(admin.call("POST","/api/v1/admin/community/comments/"+reply,Map.of("action","approve")).statusCode()).isEqualTo(200);
  String path="/api/v1/admin/community/comments/"+root;
  assertThat(viewer.call("DELETE",path,Map.of("reason","reason")).statusCode()).isEqualTo(403);
  assertThat(admin.call("DELETE",path,null).statusCode()).isEqualTo(200);
  var roots=body(viewer.call("GET","/api/v1/community/posts/"+post+"/comments/cursor",null)).path("data").path("items");
  assertThat(roots.size()).isEqualTo(1);
  assertThat(roots.get(0).path("deleted").asBoolean()).isTrue();
  assertThat(roots.get(0).path("content").asString()).isEmpty();
  assertThat(roots.get(0).path("replyCount").asLong()).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT deleted_by_user_id FROM foodtime.community_comments WHERE id=?",UUID.class,root)).isEqualTo(administrator.id());
  assertThat(jdbc.queryForObject("SELECT delete_reason FROM foodtime.community_comments WHERE id=?",String.class,root)).isNull();
  assertThat(jdbc.queryForObject("SELECT comment_count FROM foodtime.community_posts WHERE id=?",Long.class,post)).isEqualTo(1);
  assertThat(admin.call("DELETE",path,Map.of("reason","reason")).statusCode()).isEqualTo(404);
  assertThat(admin.call("DELETE","/api/v1/admin/community/comments/"+reply,Map.of("reason","违反社区规则")).statusCode()).isEqualTo(200);
  assertThat(jdbc.queryForObject("SELECT comment_count FROM foodtime.community_posts WHERE id=?",Long.class,post)).isZero();
 }

 private UUID comment(Browser browser,UUID post,UUID root)throws Exception{
  var payload=new HashMap<String,Object>();payload.put("content","评论内容");payload.put("clientRequestId",UUID.randomUUID());
  if(root!=null){payload.put("rootCommentId",root);payload.put("replyToCommentId",root);}
  var response=browser.call("POST","/api/v1/community/posts/"+post+"/comments",payload);
  assertThat(response.statusCode()).isEqualTo(202);
  return UUID.fromString(body(response).path("data").path("id").asString());
 }
}
