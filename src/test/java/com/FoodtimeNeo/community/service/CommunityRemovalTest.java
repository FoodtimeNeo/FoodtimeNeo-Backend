package com.FoodtimeNeo.community.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.dto.RemoveCommunityRequest;
import com.FoodtimeNeo.community.mapper.CommunityStore;
import com.FoodtimeNeo.community.mapper.CommunityThreadStore;
import com.FoodtimeNeo.moderation.service.ModerationService;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommunityRemovalTest {
    private final CommunityStore posts = mock(CommunityStore.class);
    private final CommunityThreadStore threads = mock(CommunityThreadStore.class);
    private final CommunityModerationService service =
            new CommunityModerationService(posts, mock(ModerationService.class), threads);
    private final UUID id = UUID.randomUUID();

    private UserProfile actor(String role, String status) {
        return new UserProfile(UUID.randomUUID(), "manager@bjtu.edu.cn", "Manager", role, status, null);
    }

    private void denied(UserProfile actor) {
        assertThatThrownBy(() -> service.remove(actor, "posts", id, new RemoveCommunityRequest("reason")))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(posts, threads);
    }

    @Test void ordinaryUsersCannotRemoveOtherPeoplesContent() { denied(actor("user", "active")); }
    @Test void unauthenticatedActorsCannotDelete() { denied(null); }

    @ParameterizedTest @ValueSource(strings = {"admin", "superadmin"})
    void inactiveAdministratorsCannotDelete(String role) { denied(actor(role, "disabled")); }

    @ParameterizedTest @ValueSource(strings = {"admin", "superadmin"})
    void administratorsRemovePostsWithTheirIdentityAndTrimmedReason(String role) {
        var actor = actor(role, "active");
        when(posts.remove(id, actor.id(), "community rules")).thenReturn(1);
        service.remove(actor, "posts", id, new RemoveCommunityRequest("  community rules  "));
        verify(posts).remove(id, actor.id(), "community rules");
        verifyNoInteractions(threads);
    }

    @ParameterizedTest @ValueSource(strings = {"admin", "superadmin"})
    void commentRemovalLocksTheParentPostBeforeUpdatingTheThread(String role) {
        var actor = actor(role, "active");
        var post = UUID.randomUUID();
        when(threads.postOf(id)).thenReturn(post);
        when(threads.lockPost(post)).thenReturn(true);
        when(threads.remove(id, actor.id(), "reason")).thenReturn(1);
        service.remove(actor, "comments", id, new RemoveCommunityRequest("reason"));
        var order = inOrder(threads);
        order.verify(threads).lockPost(post);
        order.verify(threads).remove(id, actor.id(), "reason");
    }

    @Test void deletedOrMissingContentReturns404() {
        assertThatThrownBy(() -> service.remove(actor("admin", "active"), "posts", id,
                new RemoveCommunityRequest("reason"))).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test void missingParentPostCannotBeUsedToRemoveAComment() {
        var post = UUID.randomUUID();
        when(threads.postOf(id)).thenReturn(post);
        assertThatThrownBy(() -> service.remove(actor("admin", "active"), "comments", id,
                new RemoveCommunityRequest("reason"))).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(threads, never()).remove(any(), any(), any());
    }

    @ParameterizedTest @ValueSource(strings = {"", " ", "\n\t"})
    void blankReasonsAreOptionalAndAuditStillRecordsActor(String reason) {
        var actor=actor("admin","active");
        when(posts.remove(id,actor.id(),null)).thenReturn(1);
        service.remove(actor,"posts",id,new RemoveCommunityRequest(reason));
        verify(posts).remove(id,actor.id(),null);
    }
    @Test void absentBodyAlsoAllowsAuthorizedDeletion(){
        var actor=actor("superadmin","active");when(posts.remove(id,actor.id(),null)).thenReturn(1);
        service.remove(actor,"posts",id,null);verify(posts).remove(id,actor.id(),null);
    }

    @Test void overlongReasonsAndUnsupportedTypesNeverReachStorage() {
        var actor = actor("admin", "active");
        assertThatThrownBy(() -> service.remove(actor, "posts", id, new RemoveCommunityRequest("x".repeat(501))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.remove(actor, "unknown", id, new RemoveCommunityRequest("reason")))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(posts, threads);
    }
}
