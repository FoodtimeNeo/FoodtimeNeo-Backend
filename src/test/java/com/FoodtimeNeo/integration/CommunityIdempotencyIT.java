package com.FoodtimeNeo.integration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityIdempotencyIT extends IntegrationApiSupport {
 @Test void repeatedConcurrentLikesAndFavoritesHaveExactlyOneRowAndOneCount()throws Exception{
  var author=login(owner);var viewer=login(other);var admin=login(account("admin"));
  UUID post=UUID.fromString(body(author.call("POST","/api/v1/community/posts",communityPost(author,"并发互动"))).path("data").path("id").asString());
  assertThat(admin.call("POST","/api/v1/admin/community/posts/"+post,Map.of("action","approve")).statusCode()).isEqualTo(200);
  String path="/api/v1/community/posts/"+post;
  try(var pool=Executors.newFixedThreadPool(4)){
   var tasks=new ArrayList<Callable<Integer>>();for(int i=0;i<4;i++)tasks.add(()->viewer.call("PUT",path+"/like",null).statusCode());
   for(var future:pool.invokeAll(tasks))assertThat(future.get()).isEqualTo(200);
  }
  assertThat(jdbc.queryForObject("SELECT like_count FROM foodtime.community_posts WHERE id=?",Long.class,post)).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.community_post_likes WHERE post_id=?",Long.class,post)).isEqualTo(1);
  for(int i=0;i<2;i++)assertThat(viewer.call("PUT",path+"/favorite",null).statusCode()).isEqualTo(200);
  assertThat(body(viewer.call("GET",path,null)).path("data").path("favoriteCount").asLong()).isEqualTo(1);
  for(int i=0;i<2;i++)assertThat(viewer.call("DELETE",path+"/like",null).statusCode()).isEqualTo(200);
  assertThat(jdbc.queryForObject("SELECT like_count FROM foodtime.community_posts WHERE id=?",Long.class,post)).isZero();
  assertThat(author.call("DELETE",path,null).statusCode()).isEqualTo(200);
  assertThat(viewer.call("PUT",path+"/like",null).statusCode()).isEqualTo(404);
  var favorites=body(viewer.call("GET","/api/v1/users/me/community-favorites",null)).path("data");
  assertThat(favorites.path("total").asLong()).isZero();assertThat(favorites.path("items").size()).isZero();
 }
 @Test void repliesCanBeLikedButAreUnavailableWithDeletedPost()throws Exception{
  var author=login(owner);var viewer=login(other);var admin=login(account("admin"));
  UUID post=UUID.fromString(body(author.call("POST","/api/v1/community/posts",communityPost(author,"回复点赞"))).path("data").path("id").asString());
  admin.call("POST","/api/v1/admin/community/posts/"+post,Map.of("action","approve"));
  UUID root=UUID.fromString(body(author.call("POST","/api/v1/community/posts/"+post+"/comments",Map.of("content","一级","clientRequestId",UUID.randomUUID()))).path("data").path("id").asString());
  admin.call("POST","/api/v1/admin/community/comments/"+root,Map.of("action","approve"));
  UUID reply=UUID.fromString(body(author.call("POST","/api/v1/community/posts/"+post+"/comments",Map.of("content","回复","clientRequestId",UUID.randomUUID(),"rootCommentId",root,"replyToCommentId",root))).path("data").path("id").asString());
  admin.call("POST","/api/v1/admin/community/comments/"+reply,Map.of("action","approve"));
  for(int i=0;i<2;i++)assertThat(viewer.call("PUT","/api/v1/community/comments/"+reply+"/like",null).statusCode()).isEqualTo(200);
  var page=body(viewer.call("GET","/api/v1/community/posts/"+post+"/comments/"+root+"/replies",null)).path("data").path("items").get(0);
  assertThat(page.path("likedByMe").asBoolean()).isTrue();assertThat(page.path("likeCount").asLong()).isEqualTo(1);
  author.call("DELETE","/api/v1/community/posts/"+post,null);
  assertThat(viewer.call("PUT","/api/v1/community/comments/"+reply+"/like",null).statusCode()).isEqualTo(404);
  assertThat(viewer.call("GET","/api/v1/community/posts/"+post+"/comments/cursor",null).statusCode()).isEqualTo(404);
 }
}
