package com.FoodtimeNeo.notice;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NoticeServiceTest {
    private final NoticeStore store = mock(NoticeStore.class);
    private final NoticeService service = new NoticeService(store);
    private final UUID userId = UUID.randomUUID();

    private static UserProfile actor(String role, String status) {
        return new UserProfile(UUID.randomUUID(), "a@bjtu.edu.cn", "测试", role, status, Instant.EPOCH);
    }

    @Test
    void ordinaryAdminCannotPublishAndMissingConfirmationCannotWrite() {
        assertThatThrownBy(() -> service.create(actor("admin","active"),
                new NoticeDtos.CreateNoticeRequest("t","b"))).isInstanceOf(BusinessException.class);
        UUID id=UUID.randomUUID();
        assertThatThrownBy(() -> service.confirm(userId,id,new NoticeDtos.ConfirmNoticeRequest(true)))
                .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.getCode()).isEqualTo("NOTICE_NOT_FOUND"));
        verify(store,never()).confirm(any(),any(),org.mockito.ArgumentMatchers.anyBoolean());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void feedRejectsOutOfRangeLimit(int limit) {
        assertThatThrownBy(() -> service.feed(userId, 0, limit))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(error.getStatus().value()).isEqualTo(400);
                });
        verifyNoInteractions(store);
    }

    @Test
    void feedRejectsNegativeSinceSeq() {
        assertThatThrownBy(() -> service.feed(userId, -1, 20))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(error.getStatus().value()).isEqualTo(400);
                });
        verifyNoInteractions(store);
    }

    @Test
    void feedReturnsItemsWithUnreadCount() {
        var item = new NoticeDtos.NoticeResponse(UUID.randomUUID(), "标题", "正文", Instant.now());
        when(store.feed(0, 20)).thenReturn(List.of(item));
        when(store.unreadCount(userId)).thenReturn(2L);

        var result = service.feed(userId, 0, 20);

        assertThat(result.items()).containsExactly(item);
        assertThat(result.unreadCount()).isEqualTo(2);
    }

    @Test
    void markReadOfMissingNoticeIs404WithoutWriting() {
        UUID id = UUID.randomUUID();
        when(store.exists(id)).thenReturn(false);

        assertThatThrownBy(() -> service.markRead(userId, id))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("NOTICE_NOT_FOUND");
                    assertThat(error.getStatus().value()).isEqualTo(404);
                });
        verify(store, never()).markRead(any(), any());
    }

    @Test
    void nonAdminCannotUseAdminEndpoints() {
        var page = new PageRequest(1, 20);
        var user = actor("user", "active");
        assertThatThrownBy(() -> service.adminList(user, page))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("ACCESS_DENIED");
                    assertThat(error.getStatus().value()).isEqualTo(403);
                });
        assertThatThrownBy(() -> service.adminGet(user, UUID.randomUUID()))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getCode()).isEqualTo("ACCESS_DENIED"));
        assertThatThrownBy(() -> service.create(user, new NoticeDtos.CreateNoticeRequest("t", "b")))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getCode()).isEqualTo("ACCESS_DENIED"));
        verifyNoInteractions(store);
    }

    @Test
    void disabledAdminCannotUseAdminEndpoints() {
        assertThatThrownBy(() -> service.adminList(actor("admin", "disabled"), new PageRequest(1, 20)))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("ACCESS_DENIED");
                    assertThat(error.getStatus().value()).isEqualTo(403);
                });
        verifyNoInteractions(store);
    }
}
