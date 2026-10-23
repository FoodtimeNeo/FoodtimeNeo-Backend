package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.mapper.CommunityStore;
import com.FoodtimeNeo.community.mapper.CommunityThreadStore;
import com.FoodtimeNeo.community.dto.*;
import com.FoodtimeNeo.moderation.dto.ModerationItem;
import com.FoodtimeNeo.moderation.dto.ModerationRequest;
import com.FoodtimeNeo.moderation.dto.ModerationResult;
import com.FoodtimeNeo.moderation.service.ModerationService;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.UUID;

/**
 * Community admin queue: same human/AI semantics as catalog moderation,
 * separate queue because community rows live in their own tables.
 */
@Service
public class CommunityModerationService {
    private final CommunityStore posts;
    private final ModerationService moderation;
    private final CommunityThreadStore threads;
    public CommunityModerationService(CommunityStore posts, ModerationService moderation,CommunityThreadStore threads) {
        this.posts = posts; this.moderation = moderation;this.threads=threads;
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationItem> queue(UserProfile actor, String status, PageRequest page) {
        guard(actor);
        if (!Set.of("pending", "approved", "rejected").contains(status)) {
            throw new BusinessException("VALIDATION_ERROR", "审核状态不合法", HttpStatus.BAD_REQUEST);
        }
        return PageResponse.of(posts.queue(status, page.size(), page.offset()), posts.queueCount(status), page);
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationItem> queueComments(UserProfile actor, String status, PageRequest page) {
        guard(actor);
        if (!Set.of("pending", "approved", "rejected").contains(status)) {
            throw new BusinessException("VALIDATION_ERROR", "审核状态不合法", HttpStatus.BAD_REQUEST);
        }
        return PageResponse.of(posts.queueComments(status, page.size(), page.offset()),
                posts.queueCommentCount(status), page);
    }

    @Transactional
    public ModerationResult moderate(UserProfile actor, String type, UUID id, @Valid ModerationRequest request) {
        guard(actor);
        if (!Set.of("community-post", "community-comment").contains(type)) {
            throw new BusinessException("VALIDATION_ERROR", "审核类型不合法", HttpStatus.BAD_REQUEST);
        }
        return moderation.moderate(actor, type, id, request);
    }

    private void guard(UserProfile actor) {
        if (!actor.status().equals("active") || !(actor.role().equals("admin") || actor.role().equals("superadmin"))) {
            throw new BusinessException("ACCESS_DENIED", "需要管理员权限", HttpStatus.FORBIDDEN);
        }
    }

    @Transactional(readOnly=true)
    public CommunityModerationDetail detail(UserProfile actor,String type,UUID id){
        guard(actor);
        var comment=type.equals("comments")?threads.find(id,actor.id()):null;
        UUID postId=type.equals("posts")?id:comment==null?null:comment.postId();
        var post=postId==null?null:posts.find(postId,actor.id());
        if(post==null||(comment!=null&&comment.deleted()))throw missing();
        return new CommunityModerationDetail(post,comment,comment!=null&&comment.rootCommentId()!=null?threads.find(comment.rootCommentId(),actor.id()):null,
            comment!=null&&comment.replyToCommentId()!=null?threads.find(comment.replyToCommentId(),actor.id()):null);
    }
    @Transactional
    public void remove(UserProfile actor,String type,UUID id,@Valid RemoveCommunityRequest request){
        guard(actor);int changed;
        if(type.equals("posts"))changed=posts.remove(id,actor.id(),request.reason().strip());
        else {UUID post=threads.postOf(id);if(post==null||!threads.lockPost(post))throw missing();changed=threads.remove(id,actor.id(),request.reason().strip());}
        if(changed==0)throw missing();
    }
    private BusinessException missing(){return new BusinessException("CONTENT_NOT_FOUND","社区内容不存在或已删除",HttpStatus.NOT_FOUND);}
}
