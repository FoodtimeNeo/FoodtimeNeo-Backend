package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.dto.CommunityPostResponse;
import com.FoodtimeNeo.community.dto.CreatePostRequest;
import com.FoodtimeNeo.community.mapper.CommunityStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Explicit PUT/DELETE commands are idempotent; legacy POST toggles remain compatible. */
@Service
@Validated
public class CommunityInteractionService {
    private final CommunityStore posts;
    private final CommunityPostService postService;
    private final CommunityActionRateLimiter limiter;
    public CommunityInteractionService(CommunityStore posts, CommunityPostService postService,
                                       CommunityActionRateLimiter limiter) {
        this.posts = posts; this.postService = postService; this.limiter = limiter;
    }

    @Transactional
    public Map<String, Object> toggleLike(@NotNull UUID userId, String targetType, @NotNull UUID targetId,
                                          String remoteAddress) {
        limiter.check(userId, remoteAddress);
        if (!Set.of("post", "comment").contains(targetType)) {
            throw new BusinessException("VALIDATION_ERROR", "点赞目标类型不合法", HttpStatus.BAD_REQUEST);
        }
        if (!posts.targetVisible(targetType, targetId, userId)) {
            throw new BusinessException("TARGET_NOT_FOUND", "点赞目标不存在或暂不可见", HttpStatus.NOT_FOUND);
        }
        boolean liked = posts.toggleLike(userId, targetType, targetId);
        return Map.of("liked", liked, "likeCount", posts.likeCount(targetType, targetId));
    }

    @Transactional
    public Map<String, Object> toggleFavorite(@NotNull UUID userId, @NotNull UUID postId, String remoteAddress) {
        limiter.check(userId, remoteAddress);
        if (!posts.targetVisible("post", postId, userId)) {
            throw new BusinessException("TARGET_NOT_FOUND", "动态不存在或暂不可见", HttpStatus.NOT_FOUND);
        }
        boolean active=posts.toggleFavorite(userId,postId);
        return Map.of("favorited",active,"favoriteCount",posts.postFavoriteCount(postId));
    }

    @Transactional
    public Map<String,Object> setLike(UUID user,String type,UUID id,boolean active,String address){
        limiter.check(user,address);
        if(!Set.of("post","comment").contains(type))throw new BusinessException("VALIDATION_ERROR","点赞类型不合法",HttpStatus.BAD_REQUEST);
        if(!posts.targetVisible(type,id,user))throw new BusinessException("TARGET_NOT_FOUND","点赞目标已不可见",HttpStatus.NOT_FOUND);
        posts.setLike(user,type,id,active);
        return Map.of("liked",active,"likeCount",posts.likeCount(type,id));
    }
    @Transactional
    public Map<String,Object> setFavorite(UUID user,UUID id,boolean active,String address){
        limiter.check(user,address);
        if(!posts.targetVisible("post",id,user))throw new BusinessException("TARGET_NOT_FOUND","帖子已不可见",HttpStatus.NOT_FOUND);
        posts.setFavorite(user,id,active);
        return Map.of("favorited",active,"favoriteCount",posts.postFavoriteCount(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityPostResponse> myFavorites(@NotNull UUID userId, @NotNull PageRequest page) {
        return PageResponse.of(posts.favorites(userId, page.size(), page.offset()),
                posts.favoriteCount(userId), page);
    }

    @Transactional
    public Map<String, UUID> repost(@NotNull UUID userId, @NotNull UUID postId,
                                    @Valid CreatePostRequest body, String remoteAddress) {
        return postService.submit(userId,
                new CreatePostRequest(body.content(), List.of(), postId), remoteAddress);
    }
}
