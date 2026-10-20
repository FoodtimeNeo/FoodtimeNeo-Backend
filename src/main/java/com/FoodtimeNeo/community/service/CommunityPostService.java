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
    public CommunityPostService(CommunityStore posts, ImageStorageService images,
                                SubmissionRateLimiter limiter, ApplicationEventPublisher events) {
        this.posts = posts; this.images = images; this.limiter = limiter; this.events = events;
    }

    @Transactional
    public Map<String, UUID> submit(@NotNull UUID userId, @Valid CreatePostRequest body, String remoteAddress) {
        limiter.check(userId, remoteAddress);
        for (String url : body.imageUrls()) {
            images.requireOwnedReference(url, userId);
        }
        if (body.repostOfId() != null && !posts.repostTargetVisible(body.repostOfId())) {
            throw new BusinessException("REPOST_TARGET_UNAVAILABLE", "转发的动态不存在或已不可见", HttpStatus.NOT_FOUND);
        }
        UUID id = posts.create(userId, body.content(), body.repostOfId(), body.imageUrls());
        events.publishEvent(new com.FoodtimeNeo.ai.service.SubmissionDiagnoseEvent("community-post", id));
        return Map.of("id", id);
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
