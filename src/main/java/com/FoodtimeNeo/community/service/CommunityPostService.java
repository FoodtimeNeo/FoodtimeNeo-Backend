package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.dto.CommunityPostResponse;
import com.FoodtimeNeo.community.dto.CreatePostRequest;
import com.FoodtimeNeo.community.mapper.CommunityStore;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.submission.service.SubmissionRateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.Map;
import java.util.UUID;

/**
 * Community posts: submit (pending) → AI instant triage → auto or human decision.
 * Reads only serve approved rows, except "mine" which shows the owner's own states.
 */
@Service
@Validated
public class CommunityPostService {
    private final CommunityStore posts;
    private final ImageStorageService images;
    private final SubmissionRateLimiter limiter;
    private final ApplicationEventPublisher events;
    private final boolean aiEnabled;
    public CommunityPostService(CommunityStore posts, ImageStorageService images,
                                SubmissionRateLimiter limiter, ApplicationEventPublisher events, @org.springframework.beans.factory.annotation.Value("${app.ai.triage.enabled:false}") boolean aiEnabled) {
        this.posts = posts; this.images = images; this.limiter = limiter; this.events = events; this.aiEnabled=aiEnabled;
    }

    @Transactional
    public Map<String, UUID> submit(@NotNull UUID userId, @Valid CreatePostRequest body, String remoteAddress) {
        UUID previous=posts.request(userId,body.clientRequestId());
        if(previous!=null) return retry(previous,body);
        if(body.repostOfId()==null && body.imageUrls().isEmpty())
            throw new BusinessException("POST_IMAGES_REQUIRED","请添加1到9张图片",HttpStatus.BAD_REQUEST);
        if(new java.util.HashSet<>(body.imageUrls()).size()!=body.imageUrls().size())
            throw new BusinessException("IMAGE_DUPLICATED","请勿重复添加同一张图片",HttpStatus.BAD_REQUEST);
        limiter.check(userId, remoteAddress);
        var verified=body.imageUrls().stream().map(url->images.ownedImage(url,userId)).toList();
        if (body.repostOfId() != null && !posts.repostTargetVisible(body.repostOfId())) {
            throw new BusinessException("REPOST_TARGET_UNAVAILABLE", "转发的动态不存在或已不可见", HttpStatus.NOT_FOUND);
        }
        UUID id = posts.create(userId,body,verified,aiEnabled);
        if(id==null) return retry(posts.request(userId,body.clientRequestId()),body);
        if(aiEnabled) events.publishEvent(new com.FoodtimeNeo.ai.service.SubmissionDiagnoseEvent("community-post", id));
        return Map.of("id", id);
    }

    private Map<String,UUID> retry(UUID id,CreatePostRequest body) {
        if(!posts.sameRequest(id,body)) throw new BusinessException("REQUEST_CONFLICT","请求编号已用于另一篇帖子",HttpStatus.CONFLICT);
        return Map.of("id",id);
    }

    @Transactional(readOnly=true)
    public com.FoodtimeNeo.common.api.CursorPageResponse<CommunityPostResponse> feedCursor(UUID viewerId,int size,String cursor) {
        size=Math.max(1,Math.min(100,size));
        var rows=new java.util.ArrayList<>(posts.feedAfter(viewerId,size+1,CommunityCursor.parse(cursor,"feed")));
        boolean more=rows.size()>size;if(more) rows.removeLast();
        var last=rows.isEmpty()?null:rows.getLast();
        return new com.FoodtimeNeo.common.api.CursorPageResponse<>(rows,posts.feedCount(),more,
                more?new CommunityCursor(last.createdAt(),last.id()).encode("feed"):null);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityPostResponse> feed(@NotNull UUID viewerId, @NotNull PageRequest page) {
        return PageResponse.of(posts.feed(viewerId, page.size(), page.offset()), posts.feedCount(), page);
    }

    @Transactional(readOnly = true)
    public CommunityPostResponse detail(@NotNull UUID id, @NotNull UUID viewerId) {
        CommunityPostResponse post = posts.find(id, viewerId);
        if (post == null || (!post.status().equals("approved") && !post.userId().equals(viewerId))) {
            throw new BusinessException("POST_NOT_FOUND", "动态不存在或暂不可见", HttpStatus.NOT_FOUND);
        }
        return post;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityPostResponse> mine(@NotNull UUID userId, @NotNull PageRequest page) {
        return PageResponse.of(posts.mine(userId, page.size(), page.offset()), posts.mineCount(userId), page);
    }

    @Transactional
    public void delete(@NotNull UUID id, @NotNull UUID userId) {
        if (posts.deleteOwned(id, userId) == 0) {
            throw new BusinessException("POST_NOT_FOUND", "动态不存在或不属于你", HttpStatus.NOT_FOUND);
        }
    }
}
