package com.FoodtimeNeo.integration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityThreadIT extends IntegrationApiSupport {
 @Test void compactRepliesKeepTargetsAndRootDeletionPreservesValidChildren()throws Exception{
  var author=login(owner);var viewer=login(other);var admin=login(account("admin"));
  UUID post=UUID.fromString(body(author.call("POST","/api/v1/community/posts",communityPost(author,"楼中楼"))).path("data").path("id").asString());
  approve(admin,"posts",post);
  UUID root=comment(author,post,"一级评论",null,null);approve(admin,"comments",root);
  UUID first=comment(viewer,post,"回复一级",root,root);approve(admin,"comments",first);
  UUID second=comment(author,post,"楼内互相回复",root,first);approve(admin,"comments",second);
  var replies=body(viewer.call("GET","/api/v1/community/posts/"+post+"/comments/"+root+"/replies?size=1",null)).path("data");
  assertThat(replies.path("items").get(0).path("id").asString()).isEqualTo(first.toString());
  assertThat(replies.path("hasMore").asBoolean()).isTrue();
  var next=body(viewer.call("GET","/api/v1/community/posts/"+post+"/comments/"+root+"/replies?size=1&cursor="+replies.path("nextCursor").asString(),null)).path("data").path("items").get(0);
  assertThat(next.path("replyToCommentId").asString()).isEqualTo(first.toString());
  assertThat(next.path("replyToUserId").asString()).isEqualTo(other.id().toString());
  assertThat(jdbc.queryForObject("SELECT comment_count FROM foodtime.community_posts WHERE id=?",Long.class,post)).isEqualTo(3);
  assertThat(author.call("DELETE","/api/v1/community/comments/"+root,null).statusCode()).isEqualTo(200);
  var roots=body(viewer.call("GET","/api/v1/community/posts/"+post+"/comments/cursor",null)).path("data").path("items");
  assertThat(roots.size()).isEqualTo(1);assertThat(roots.get(0).path("deleted").asBoolean()).isTrue();
  assertThat(roots.get(0).path("content").asString()).isEmpty();assertThat(roots.get(0).path("replyCount").asLong()).isEqualTo(2);
  assertThat(viewer.call("POST","/api/v1/community/posts/"+post+"/comments",Map.of("clientRequestId",UUID.randomUUID(),"content","不能追加","rootCommentId",root,"replyToCommentId",first)).statusCode()).isEqualTo(409);
  assertThat(jdbc.queryForObject("SELECT comment_count FROM foodtime.community_posts WHERE id=?",Long.class,post)).isEqualTo(2);
 }
 @Test void wrongThreadIsRejectedAndRetriesDoNotDuplicate()throws Exception{
  var author=login(owner);var admin=login(account("admin"));
  UUID post=UUID.fromString(body(author.call("POST","/api/v1/community/posts",communityPost(author,"关系校验"))).path("data").path("id").asString());approve(admin,"posts",post);
  UUID one=comment(author,post,"甲楼",null,null);approve(admin,"comments",one);
  UUID two=comment(author,post,"乙楼",null,null);approve(admin,"comments",two);
  var payload=Map.of("clientRequestId",UUID.randomUUID(),"content","重试评论");
  var a=author.call("POST","/api/v1/community/posts/"+post+"/comments",payload);
  var b=author.call("POST","/api/v1/community/posts/"+post+"/comments",payload);
  assertThat(a.statusCode()).isEqualTo(202);assertThat(body(a).path("data").path("id").asString()).isEqualTo(body(b).path("data").path("id").asString());
  assertThat(author.call("POST","/api/v1/community/posts/"+post+"/comments",Map.of("clientRequestId",UUID.randomUUID(),"content","跨楼回复","rootCommentId",one,"replyToCommentId",two)).statusCode()).isEqualTo(409);
 }
 private UUID comment(Browser b,UUID post,String text,UUID root,UUID target)throws Exception{
  var data=new HashMap<String,Object>();data.put("content",text);data.put("clientRequestId",UUID.randomUUID());
  if(root!=null){data.put("rootCommentId",root);data.put("replyToCommentId",target);}
  var response=b.call("POST","/api/v1/community/posts/"+post+"/comments",data);assertThat(response.statusCode()).isEqualTo(202);
  return UUID.fromString(body(response).path("data").path("id").asString());
 }
 private void approve(Browser b,String type,UUID id)throws Exception{assertThat(b.call("POST","/api/v1/admin/community/"+type+"/"+id,Map.of("action","approve")).statusCode()).isEqualTo(200);}
}
