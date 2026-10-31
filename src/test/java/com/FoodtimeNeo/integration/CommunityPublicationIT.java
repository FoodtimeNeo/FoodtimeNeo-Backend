package com.FoodtimeNeo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityPublicationIT extends IntegrationApiSupport {
 @Test void retriesAreDeduplicatedAndConflictingPayloadIsRejected() throws Exception {
  var author=login(owner);var payload=new HashMap<>(communityPost(author,"网络重试"));
  var first=author.call("POST","/api/v1/community/posts",payload);
  var again=author.call("POST","/api/v1/community/posts",payload);
  assertThat(first.statusCode()).isEqualTo(202);assertThat(again.statusCode()).isEqualTo(202);
  var id=body(first).path("data").path("id").asString();
  assertThat(body(again).path("data").path("id").asString()).isEqualTo(id);
  payload.put("content","不同正文");
  assertThat(author.call("POST","/api/v1/community/posts",payload).statusCode()).isEqualTo(409);
  var detail=body(author.call("GET","/api/v1/community/posts/"+id,null)).path("data");
  assertThat(detail.path("title").asString()).isEqualTo("食记");
  assertThat(detail.path("images").get(0).path("width").asInt()).isEqualTo(3);
  assertThat(detail.path("images").get(0).path("height").asInt()).isEqualTo(5);
  assertThat(detail.path("reviewStage").asString()).isEqualTo("manual_pending");
  assertThat(detail.path("manualReviewReason").asString()).isEqualTo("ai_disabled");
 }
 @Test void publicationControlsImageAccessAndDeletionKeepsEvidence() throws Exception {
  var author=login(owner);var viewer=login(other);var admin=login(account("admin"));
  var payload=communityPost(author,"图片授权");
  var image=((List<?>)payload.get("imageUrls")).getFirst().toString();
  UUID id=UUID.fromString(body(author.call("POST","/api/v1/community/posts",payload)).path("data").path("id").asString());
  assertThat(viewer.image(image).statusCode()).isEqualTo(404);
  assertThat(admin.call("POST","/api/v1/admin/community/posts/"+id,Map.of("action","approve")).statusCode()).isEqualTo(200);
  assertThat(viewer.image(image).statusCode()).isEqualTo(200);
  var publishedImage=viewer.image(image);
  assertThat(publishedImage.headers().firstValue("Cache-Control").orElseThrow()).contains("private", "no-cache");
  assertThat(publishedImage.headers().allValues("Vary").toString()).contains("Cookie");
  var conditional=viewer.imageIfNoneMatch(image,publishedImage.headers().firstValue("ETag").orElseThrow());
  assertThat(conditional.statusCode()).isEqualTo(304);
  assertThat(conditional.headers().firstValue("Cache-Control").orElseThrow()).contains("private", "no-cache");
  assertThatThrownBy(()->jdbc.update("UPDATE foodtime.community_posts SET title='修改' WHERE id=?",id)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  assertThat(author.call("DELETE","/api/v1/community/posts/"+id,null).statusCode()).isEqualTo(200);
  assertThat(viewer.call("GET","/api/v1/community/posts/"+id,null).statusCode()).isEqualTo(404);
  assertThat(viewer.image(image).statusCode()).isEqualTo(404);
  assertThat(viewer.imageIfNoneMatch(image,publishedImage.headers().firstValue("ETag").orElseThrow()).statusCode()).isEqualTo(404);
  assertThat(author.image(image).headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
  assertThat(author.imageIfNoneMatch(image,publishedImage.headers().firstValue("ETag").orElseThrow()).statusCode()).isEqualTo(200);
  assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM foodtime.community_posts WHERE id=?",Boolean.class,id)).isTrue();
  assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.content_moderation_logs WHERE community_post_id=?",Long.class,id)).isEqualTo(1);
 }
 @Test void cursorAndImageContractRejectInvalidInput()throws Exception {
  var author=login(owner);
  assertThat(author.call("GET","/api/v1/community/feed/cursor?cursor=bad",null).statusCode()).isEqualTo(400);
  assertThat(author.call("POST","/api/v1/community/posts",Map.of("title","标题","content","正文","clientRequestId",UUID.randomUUID())).statusCode()).isEqualTo(400);
 }
}
