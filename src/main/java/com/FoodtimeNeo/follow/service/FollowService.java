package com.FoodtimeNeo.follow.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.follow.FollowDtos.FollowStateResponse;
import com.FoodtimeNeo.follow.FollowDtos.FollowUserResponse;
import com.FoodtimeNeo.follow.mapper.FollowStore;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Directed follow relation; lists are readable by any signed-in user, writes only affect the caller. */
@Service
public class FollowService {
    private final FollowStore store;
    private final FollowRateLimiter limiter;

    public FollowService(FollowStore store, FollowRateLimiter limiter) {
        this.store = store;
        this.limiter = limiter;
    }

    @Transactional
    public FollowStateResponse follow(UserProfile actor, UUID target, String remoteAddress) {
        requireTarget(actor, target);
        limiter.check(actor.id(), remoteAddress);
        store.follow(actor.id(), target);
        return new FollowStateResponse(true, store.followerCountVisible(target));
    }

    @Transactional
    public FollowStateResponse unfollow(UserProfile actor, UUID target, String remoteAddress) {
        requireTarget(actor, target);
        limiter.check(actor.id(), remoteAddress);
        store.unfollow(actor.id(), target);
        return new FollowStateResponse(false, store.followerCountVisible(target));
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public FollowStateResponse status(UserProfile actor, UUID target) {
        requireTarget(actor, target);
        return new FollowStateResponse(store.follows(actor.id(), target), store.followerCountVisible(target));
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<FollowUserResponse> followers(UUID userId, UserProfile viewer, PageRequest page) {
        requireVisible(userId);
        return PageResponse.of(store.followers(userId, viewer.id(), page.size(), page.offset()),
                store.followerCountVisible(userId), page);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<FollowUserResponse> following(UUID userId, UserProfile viewer, PageRequest page) {
        requireVisible(userId);
        return PageResponse.of(store.following(userId, viewer.id(), page.size(), page.offset()),
                store.followingCountVisible(userId), page);
    }

    private void requireTarget(UserProfile actor, UUID target) {
        if (actor.id().equals(target)) {
            throw new BusinessException("CANNOT_FOLLOW_SELF", "不能关注自己", HttpStatus.BAD_REQUEST);
        }
        if (!store.targetActive(target)) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在或已停用", HttpStatus.NOT_FOUND);
        }
    }

    private void requireVisible(UUID userId) {
        if (!store.targetActive(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在或已停用", HttpStatus.NOT_FOUND);
        }
    }
}
