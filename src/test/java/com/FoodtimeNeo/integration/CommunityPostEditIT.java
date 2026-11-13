package com.FoodtimeNeo.integration;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.moderation.service.ModerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommunityPostEditIT extends IntegrationApiSupport {
    @Autowired ModerationService moderation;
    @Autowired com.FoodtimeNeo.moderation.mapper.ModerationStore moderationStore;
    @Autowired com.FoodtimeNeo.media.service.StagedImageCleanup cleanup;
    private String path(UUID id){return "/api/v1/community/posts/"+id;}
    private UUID create(Browser author,Map<String,Object> input)throws Exception{
        var result=author.call("POST","/api/v1/community/posts",input);
        assertThat(result.statusCode()).as(result.body()).isEqualTo(202);
        return UUID.fromString(body(result).path("data").path("id").asString());
    }
    private Map<String,Object> edit(Map<String,Object> original,long revision,String title){
        var input=new LinkedHashMap<>(original);input.remove("repostOfId");
        input.put("title",title);input.put("expectedRevision",revision);input.put("clientRequestId",UUID.randomUUID());return input;
    }
    private void decide(Browser admin,UUID id,long revision,String action)throws Exception{
        var result=admin.call("POST","/api/v1/admin/community/posts/"+id,Map.of("action",action,"reason","人工核验","contentRevision",revision));
        assertThat(result.statusCode()).as(result.body()).isEqualTo(200);
    }
    @Test void editHidesWholePostAndRestoresPinWithoutLosingIdentityOrInteractions()throws Exception{
        var author=login(owner);var viewer=login(other);var admin=login(account("superadmin"));
        var original=communityPost(author,"原始正文");UUID id=create(author,original);decide(admin,id,1,"approve");
        assertThat(viewer.call("PUT",path(id)+"/like",null).statusCode()).isEqualTo(200);
        assertThat(viewer.call("PUT",path(id)+"/favorite",null).statusCode()).isEqualTo(200);
        assertThat(viewer.call("POST",path(id)+"/views",Map.of("clientRequestId",UUID.randomUUID())).statusCode()).isEqualTo(200);
        UUID comment=UUID.randomUUID();jdbc.update("INSERT INTO foodtime.community_comments(id,post_id,user_id,content) VALUES(?,?,?,'保留评论')",comment,id,other.id());
        long pinRevision=body(admin.call("GET","/api/v1/community/pins",null)).path("data").path("revision").asLong();
        assertThat(admin.call("PUT","/api/v1/admin/community/pins/"+id,Map.of("expectedRevision",pinRevision)).statusCode()).isEqualTo(200);
        var before=jdbc.queryForMap("SELECT created_at,like_count,comment_count,favorite_count,view_count FROM foodtime.community_posts WHERE id=?",id);
        var input=edit(original,1,"补充标题");input.put("content","更新正文");input.put("dishId",dish);
        assertThat(author.call("PUT",path(id),input).statusCode()).isEqualTo(200);
        assertThat(viewer.call("GET",path(id),null).statusCode()).isEqualTo(404);
        assertThat(body(admin.call("GET","/api/v1/community/pins",null)).path("data").path("items")).isEmpty();
        assertThat(body(admin.call("GET","/api/v1/admin/community/pins",null)).path("data").path("items")).hasSize(1);
        var own=body(author.call("GET",path(id),null)).path("data");
        assertThat(own.path("contentRevision").asLong()).isEqualTo(2);assertThat(own.path("editedAt").isNull()).isFalse();
        assertThat(own.path("content").asString()).isEqualTo("更新正文");
        assertThat(jdbc.queryForMap("SELECT created_at,like_count,comment_count,favorite_count,view_count FROM foodtime.community_posts WHERE id=?",id)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.community_comments WHERE id=?",Integer.class,comment)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT payload->'post'->>'content' FROM foodtime.community_post_versions WHERE post_id=? AND revision=1",String.class,id)).isEqualTo("原始正文");
        assertThat(jdbc.queryForObject("SELECT post_content_snapshot->'post'->>'content' FROM foodtime.content_moderation_logs WHERE community_post_id=?",String.class,id)).isEqualTo("原始正文");
        decide(admin,id,2,"approve");
        assertThat(viewer.call("GET",path(id),null).statusCode()).isEqualTo(200);
        assertThat(body(admin.call("GET","/api/v1/community/pins",null)).path("data").path("items").get(0).path("id").asString()).isEqualTo(id.toString());
    }
    @Test void latestPendingSubmissionSupersedesOldHumanReviewAndRejectedPostMayBeEditedAgain()throws Exception{
        var author=login(owner);var admin=login(account("admin"));var original=communityPost(author,"正文");UUID id=create(author,original);
        assertThat(author.call("PUT",path(id),edit(original,1,"第二版")).statusCode()).isEqualTo(200);
        for(var stale:List.of(Map.of("action","approve"),Map.of("action","approve","contentRevision",1)))
            assertThat(admin.call("POST","/api/v1/admin/community/posts/"+id,stale).statusCode()).isEqualTo(409);
        assertThat(author.call("PUT",path(id),edit(original,2,"第三版")).statusCode()).isEqualTo(200);
        decide(admin,id,3,"reject");
        assertThat(author.call("PUT",path(id),edit(original,3,"第四版")).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT title FROM foodtime.community_posts WHERE id=?",String.class,id)).isEqualTo("第四版");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.community_post_versions WHERE post_id=?",Integer.class,id)).isEqualTo(3);
    }
    @Test void requestRetriesAreIdempotentAndUnchangedContentDoesNotHideApprovedPost()throws Exception{
        var author=login(owner);var admin=login(account("admin"));var original=communityPost(author,"正文");UUID id=create(author,original);decide(admin,id,1,"approve");
        var noop=edit(original,1,"食记");var unchanged=author.call("PUT",path(id),noop);
        assertThat(unchanged.statusCode()).isEqualTo(200);assertThat(body(unchanged).path("data").path("changed").asBoolean()).isFalse();
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.community_posts WHERE id=?",String.class,id)).isEqualTo("approved");
        var input=edit(original,1,"新标题");var first=author.call("PUT",path(id),input);assertThat(first.statusCode()).isEqualTo(200);
        assertThat(body(author.call("PUT",path(id),input)).path("data")).isEqualTo(body(first).path("data"));
        assertThat(author.call("PUT",path(id),edit(original,1,"旧版本")).statusCode()).isEqualTo(409);
        input.put("title","改用同一请求编号");assertThat(author.call("PUT",path(id),input).statusCode()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT content_revision FROM foodtime.community_posts WHERE id=?",Long.class,id)).isEqualTo(2);
    }
    @Test void creationReceiptSurvivesEditsAndStillRejectsDifferentCreationPayload()throws Exception{
        var author=login(owner);var original=communityPost(author,"原始正文");UUID id=create(author,original);
        assertThat(author.call("PUT",path(id),edit(original,1,"新的标题")).statusCode()).isEqualTo(200);
        assertThat(create(author,original)).isEqualTo(id);
        var different=new LinkedHashMap<>(original);different.put("content","不同正文");
        assertThat(author.call("POST","/api/v1/community/posts",different).statusCode()).isEqualTo(409);
    }
    @Test void ownershipDeletedStateAndInvalidReplacementCannotPartiallyChangeAPost()throws Exception{
        var author=login(owner);var stranger=login(other);var admin=login(account("superadmin"));
        var original=communityPost(author,"正文");UUID id=create(author,original);var input=edit(original,1,"新标题");
        for(var actor:List.of(stranger,admin))assertThat(actor.call("PUT",path(id),input).statusCode()).isEqualTo(404);
        assertThat(new Browser().call("PUT",path(id),input).statusCode()).isIn(401,403);
        var foreign=new LinkedHashMap<>(input);foreign.put("imageUrls",communityPost(stranger,"借用图片").get("imageUrls"));
        var refused=author.call("PUT",path(id),foreign);
        assertThat(refused.statusCode()).isEqualTo(400);assertThat(body(refused).path("message").asString()).contains("本人上传");
        var noImages=new LinkedHashMap<>(input);noImages.put("imageUrls",List.of());assertThat(author.call("PUT",path(id),noImages).statusCode()).isEqualTo(400);
        var badDish=new LinkedHashMap<>(input);badDish.put("dishId",UUID.randomUUID());assertThat(author.call("PUT",path(id),badDish).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT content_revision FROM foodtime.community_posts WHERE id=?",Long.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.community_post_versions WHERE post_id=?",Integer.class,id)).isZero();
        assertThat(author.call("DELETE",path(id),null).statusCode()).isEqualTo(200);
        assertThat(author.call("PUT",path(id),input).statusCode()).isEqualTo(404);
    }
    @Test void concurrentEditsAcceptExactlyOneVersion()throws Exception{
        var author=login(owner);var second=login(owner);var original=communityPost(author,"正文");UUID id=create(author,original);
        var barrier=new CyclicBarrier(2);var a=edit(original,1,"并发甲");var b=edit(original,1,"并发乙");
        try(var pool=Executors.newFixedThreadPool(2)){
            var x=pool.submit(()->{barrier.await();return author.call("PUT",path(id),a).statusCode();});
            var y=pool.submit(()->{barrier.await();return second.call("PUT",path(id),b).statusCode();});
            assertThat(List.of(x.get(15,TimeUnit.SECONDS),y.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(jdbc.queryForObject("SELECT content_revision FROM foodtime.community_posts WHERE id=?",Long.class,id)).isEqualTo(2);
    }
    @Test void editedRepostKeepsSourceAndDoesNotCountAsAnotherRepostOnApproval()throws Exception{
        var author=login(owner);var admin=login(account("admin"));var original=communityPost(author,"原始帖子");UUID source=create(author,original);decide(admin,source,1,"approve");
        var repost=new LinkedHashMap<>(communityPost(author,"转发意见"));repost.put("repostOfId",source);repost.put("imageUrls",List.of());UUID id=create(author,repost);decide(admin,id,1,"approve");
        var replacement=edit(repost,1,"修改转发意见");replacement.put("imageUrls",original.get("imageUrls"));
        assertThat(author.call("PUT",path(id),replacement).statusCode()).isEqualTo(200);
        assertThat(moderationStore.logs(100,0).stream().filter(log->id.equals(log.submissionId())).toList())
            .singleElement().satisfies(log->{assertThat(log.imageUrl()).isNull();assertThat(log.content()).isEqualTo("转发意见");});
        decide(admin,id,2,"approve");
        assertThat(jdbc.queryForObject("SELECT repost_of_id FROM foodtime.community_posts WHERE id=?",UUID.class,id)).isEqualTo(source);
        assertThat(jdbc.queryForObject("SELECT repost_count FROM foodtime.community_posts WHERE id=?",Integer.class,source)).isEqualTo(1);
    }
    @Test void lateAiResultsCannotPersistOrApproveAReplacementVersion()throws Exception{
        var author=login(owner);var original=communityPost(author,"旧正文");UUID id=create(author,original);
        UUID oldImage=jdbc.queryForObject("SELECT id FROM foodtime.community_post_images WHERE post_id=?",UUID.class,id);
        assertThat(author.call("PUT",path(id),edit(original,1,"新版本")).statusCode()).isEqualTo(200);
        jdbc.update("UPDATE foodtime.community_posts SET review_stage='ai_pending',manual_review_reason=NULL WHERE id=?",id);
        var verdict=new AiVerdict("LOW",0.99,List.of(),"旧请求返回","fake");
        assertThat(moderation.recordPostVerdict(id,1,verdict)).isEqualTo(-1);
        moderation.recordPostImageVerdict(id,1,oldImage,verdict);
        assertThat(moderation.autoDecidePost(id,1,verdict,true)).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM foodtime.community_posts WHERE id=?",String.class,id)).isEqualTo("pending");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foodtime.ai_moderation_results WHERE submission_id IN(?,?)",Integer.class,id,oldImage)).isZero();
        assertThat(moderation.recordPostVerdict(id,2,verdict)).isEqualTo(1);
        assertThat(moderation.autoDecidePost(id,2,verdict,true)).isNotNull();
    }
    @Test void removedImageIsRetainedAsVersionEvidenceWhileAnUnreferencedUploadIsCleaned()throws Exception{
        var author=login(owner);var original=communityPost(author,"原图正文");UUID id=create(author,original);
        var replacement=edit(communityPost(author,"新图正文"),1,"替换图片");
        assertThat(author.call("PUT",path(id),replacement).statusCode()).isEqualTo(200);
        String old=((List<String>)original.get("imageUrls")).get(0);
        String orphan=((List<String>)communityPost(author,"未关联图片").get("imageUrls")).get(0);
        for(String reference:List.of(old,orphan))for(String suffix:List.of(".img",".properties")){
            var file=integrationStorageDirectory().resolve(reference.substring(reference.lastIndexOf('/')+1)+suffix);
            java.nio.file.Files.setLastModifiedTime(file,java.nio.file.attribute.FileTime.from(java.time.Instant.EPOCH));
        }
        cleanup.clean();
        assertThat(java.nio.file.Files.exists(integrationStorageDirectory().resolve(old.substring(old.lastIndexOf('/')+1)+".img"))).isTrue();
        assertThat(java.nio.file.Files.exists(integrationStorageDirectory().resolve(orphan.substring(orphan.lastIndexOf('/')+1)+".img"))).isFalse();
    }
}
