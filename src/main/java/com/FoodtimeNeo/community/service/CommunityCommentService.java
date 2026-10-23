package com.FoodtimeNeo.community.service;
import com.FoodtimeNeo.ai.service.SubmissionDiagnoseEvent;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.dto.*;
import com.FoodtimeNeo.community.mapper.*;
import com.FoodtimeNeo.submission.service.SubmissionRateLimiter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.*;
@Service @Validated
public class CommunityCommentService {
 private final CommunityStore posts;
 private final CommunityThreadStore threads;
 private final SubmissionRateLimiter limiter;
 private final ApplicationEventPublisher events;
 private final boolean ai;
 public CommunityCommentService(CommunityStore posts,CommunityThreadStore threads,SubmissionRateLimiter limiter,ApplicationEventPublisher events,@Value("${app.ai.triage.enabled:false}") boolean ai){this.posts=posts;this.threads=threads;this.limiter=limiter;this.events=events;this.ai=ai;}
 @Transactional public Map<String,UUID> submit(UUID user,UUID post,@Valid CreateCommentRequest body,String address){
  UUID prior=threads.retry(user,post,body);if(prior!=null)return Map.of("id",prior);
  if(!threads.lockPost(post))throw missing();
  limiter.check(user,address);
  UUID id=threads.create(user,post,body,ai);
  if(id==null)return Map.of("id",threads.retry(user,post,body));
  if(ai)events.publishEvent(new SubmissionDiagnoseEvent("community-comment",id));
  return Map.of("id",id);
 }
 @Transactional(readOnly=true) public PageResponse<CommunityCommentResponse> list(UUID post,PageRequest page){
  if(!posts.postVisibleForComment(post))throw missing();
  return PageResponse.of(threads.legacy(post,page.size(),page.offset()),posts.commentCount(post),page);
 }
 @Transactional(readOnly=true) public CursorPageResponse<CommunityCommentResponse> thread(UUID post,UUID root,UUID viewer,int size,String cursor){
  if(!posts.postVisibleForComment(post))throw missing();
  if(root!=null&&!threads.rootVisible(root,post,viewer))throw missing();
  size=Math.max(1,Math.min(100,size));String scope=root==null?"comments:"+post:"replies:"+root;
  var rows=new ArrayList<>(threads.page(post,root,viewer,size+1,CommunityCursor.parse(cursor,scope)));
  boolean more=rows.size()>size;if(more)rows.removeLast();
  var last=rows.isEmpty()?null:rows.getLast();
  return new CursorPageResponse<>(rows,threads.total(post,root,viewer),more,more?new CommunityCursor(last.createdAt(),last.id()).encode(scope):null);
 }
 @Transactional public void delete(UUID id,UUID user){
  UUID post=threads.postOf(id);
  if(post==null||!threads.lockPost(post)||threads.delete(id,user)==0)throw missing();
 }
 private BusinessException missing(){return new BusinessException("COMMENT_NOT_FOUND","帖子或评论不存在或暂不可见",HttpStatus.NOT_FOUND);}
}
