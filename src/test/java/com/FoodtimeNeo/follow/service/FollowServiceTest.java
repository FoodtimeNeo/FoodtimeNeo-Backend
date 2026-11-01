package com.FoodtimeNeo.follow.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.follow.FollowDtos.FollowStateResponse;
import com.FoodtimeNeo.follow.FollowDtos.FollowUserResponse;
import com.FoodtimeNeo.follow.mapper.FollowStore;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FollowServiceTest {
    private static final String IP = "127.0.0.1";
    private final FollowStore store = mock(FollowStore.class);
    private final FollowRateLimiter limiter = mock(FollowRateLimiter.class);
    private final FollowService service = new FollowService(store, limiter);
    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private final UserProfile actor = new UserProfile(actorId, "a@bjtu.edu.cn", "我", "user", "active", null);

    private static String codeOf(Throwable error) {
        return ((BusinessException) error).getCode();
    }

    @Test
    void followingYourselfIsRejectedBeforeAnyWrite() {
        assertThatThrownBy(() -> service.follow(actor, actorId, IP))
                .isInstanceOf(BusinessException.class)
                .extracting(FollowServiceTest::codeOf)
                .isEqualTo("CANNOT_FOLLOW_SELF");
        verify(store, never()).follow(any(), any());
    }

    @Test
    void unknownTargetIsReportedAsNotFound() {
        when(store.targetActive(targetId)).thenReturn(false);
        assertThatThrownBy(() -> service.follow(actor, targetId, IP))
                .isInstanceOf(BusinessException.class)
                .extracting(FollowServiceTest::codeOf)
                .isEqualTo("USER_NOT_FOUND");
        verify(store, never()).follow(any(), any());
    }

    @Test
    void followIsThrottledThenPersisted() {
        when(store.targetActive(targetId)).thenReturn(true);
        when(store.followerCountVisible(targetId)).thenReturn(7L);

        FollowStateResponse state = service.follow(actor, targetId, IP);

        verify(limiter).check(actorId, IP);
        verify(store).follow(actorId, targetId);
        assertThat(state.followed()).isTrue();
        assertThat(state.followerCount()).isEqualTo(7L);
    }

    @Test
    void unfollowRemovesTheRelation() {
        when(store.targetActive(targetId)).thenReturn(true);
        when(store.followerCountVisible(targetId)).thenReturn(6L);

        FollowStateResponse state = service.unfollow(actor, targetId, IP);

        verify(store).unfollow(actorId, targetId);
        assertThat(state.followed()).isFalse();
        assertThat(state.followerCount()).isEqualTo(6L);
    }

    @Test
    void statusReflectsTheStoredRelation() {
        when(store.targetActive(targetId)).thenReturn(true);
        when(store.follows(actorId, targetId)).thenReturn(true);
        when(store.followerCountVisible(targetId)).thenReturn(3L);

        assertThat(service.status(actor, targetId).followed()).isTrue();
    }

    @Test
    void listsArePagedAndMapped() {
        when(store.targetActive(targetId)).thenReturn(true);
        FollowUserResponse row = new FollowUserResponse(UUID.randomUUID(), "同学", null, true);
        when(store.followers(targetId, actorId, 20, 0L)).thenReturn(List.of(row));
        when(store.followerCountVisible(targetId)).thenReturn(1L);

        var page = service.followers(targetId, actor, new PageRequest(1, 20));

        assertThat(page.items()).containsExactly(row);
        assertThat(page.total()).isEqualTo(1L);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(20);
    }

    @Test
    void listForAnInactiveUserIsNotFound() {
        when(store.targetActive(targetId)).thenReturn(false);
        assertThatThrownBy(() -> service.following(targetId, actor, new PageRequest(1, 20)))
                .isInstanceOf(BusinessException.class)
                .extracting(FollowServiceTest::codeOf)
                .isEqualTo("USER_NOT_FOUND");
        verify(store, never()).following(any(), any(), anyInt(), anyLong());
    }
}
