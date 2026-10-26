package com.FoodtimeNeo.message;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageServiceTest {
    private final MessageStore store = mock(MessageStore.class);
    private final MessageService service = new MessageService(store);
    private final UUID userId = UUID.randomUUID();

    @Test
    void markReadOfUnownedOrMissingMessageIs404WithoutWriting() {
        UUID id = UUID.randomUUID();
        when(store.owned(id, userId)).thenReturn(false);

        assertThatThrownBy(() -> service.markRead(userId, id))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("MESSAGE_NOT_FOUND");
                    assertThat(error.getStatus().value()).isEqualTo(404);
                });
        verify(store, never()).markRead(any(), any());
    }

    @Test
    void markReadOfOwnedMessageDelegatesToStore() {
        UUID id = UUID.randomUUID();
        when(store.owned(id, userId)).thenReturn(true);

        service.markRead(userId, id);

        verify(store).markRead(id, userId);
    }

    @Test
    void markAllReadOnlyTouchesCurrentUser() {
        service.markAllRead(userId);

        verify(store).markAllRead(eq(userId));
        verify(store, never()).markRead(any(), any());
    }

    @Test
    void listAndUnreadCountDelegateWithPageBounds() {
        var item = new MessageDtos.MessageResponse(
                UUID.randomUUID(), "标题", "正文", "SYSTEM", null, null, false, Instant.now());
        var page = new PageRequest(1, 20);
        when(store.page(userId, 20, 0)).thenReturn(List.of(item));
        when(store.count(userId)).thenReturn(1L);
        when(store.unreadCount(userId)).thenReturn(1L);

        assertThat(service.list(userId, page).items()).containsExactly(item);
        assertThat(service.unreadCount(userId).unreadCount()).isEqualTo(1);
    }
}
