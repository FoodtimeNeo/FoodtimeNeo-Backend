package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.mapper.CommunityStore;
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
    public CommunityModerationService(CommunityStore posts, ModerationService moderation) {
        this.posts = posts; this.moderation = moderation;
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
}
