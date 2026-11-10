package com.FoodtimeNeo.message;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class DirectMessageServiceTest {
    private final DirectMessageStore store = mock(DirectMessageStore.class);
    private final DirectMessageRateLimiter limiter = mock(DirectMessageRateLimiter.class);
    private final DirectMessageService service = new DirectMessageService(store, limiter,
            mock(com.FoodtimeNeo.safety.SafetyStore.class));
    private final UUID me = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private final UUID other = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private final UUID conversation = UUID.fromString("33333333-3333-4333-8333-333333333333");

    private DirectMessageStore.ConversationRow row() {
        return new DirectMessageStore.ConversationRow(conversation, me, other, null, null, null, null);
    }

    private DirectMessageDtos.ConversationResponse summary() {
        return new DirectMessageDtos.ConversationResponse(conversation,
                new DirectMessageDtos.Participant(other, "对方", null), null, null, null, 0,
                Instant.parse("2026-11-06T00:00:00Z"));
    }

    @Test
    void startingASelfConversationIsRejected() {
        assertThatThrownBy(() -> service.start(me, me))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("SELF_CONVERSATION");
        verifyNoInteractions(store);
    }

    @Test
    void startOrdersThePairAndMarksItRead() {
        when(store.activeUser(other)).thenReturn(true);
        when(store.findPair(me, other)).thenReturn(Optional.of(row()));
        when(store.summary(conversation, me)).thenReturn(Optional.of(summary()));

        assertThat(service.start(me, other).id()).isEqualTo(conversation);
        verify(store).findPair(me, other);
        verify(store).markRead(conversation, me);
    }

    @Test
    void sendTrimsContentAndIsIdempotentByClientRequest() {
        UUID request = UUID.randomUUID();
        var message = new DirectMessageDtos.MessageResponse(UUID.randomUUID(), conversation, me,
                "你好", Instant.parse("2026-11-06T00:00:00Z"));
        when(store.find(conversation)).thenReturn(Optional.of(row()));
        when(store.insertMessage(any(), eq(conversation), eq(me), eq("你好"), eq(request))).thenReturn(1);
        when(store.findByClientRequest(me, request)).thenReturn(Optional.of(message));

        var result = service.send(me, conversation,
                new DirectMessageDtos.SendMessageRequest("  你好  ", request), "127.0.0.1");

        assertThat(result.content()).isEqualTo("你好");
        verify(limiter).check(me, "127.0.0.1");
        verify(store).revive(conversation);
    }

    @Test
    void nonMemberCannotReadTheConversation() {
        when(store.find(conversation)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.messages(me, conversation, 30, null))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("CONVERSATION_NOT_FOUND");
    }

    @Test
    void messagePageReturnsAscendingItemsAndPointsAtOlderMessages() {
        when(store.find(conversation)).thenReturn(Optional.of(row()));
        Instant base = Instant.parse("2026-11-06T00:00:00Z");
        var older = new DirectMessageDtos.MessageResponse(UUID.randomUUID(), conversation, other,
                "较早", base.minusSeconds(10));
        var middle = new DirectMessageDtos.MessageResponse(UUID.randomUUID(), conversation, me,
                "中间", base);
        var newest = new DirectMessageDtos.MessageResponse(UUID.randomUUID(), conversation, other,
                "最新", base.plusSeconds(10));
        when(store.messages(eq(conversation), isNull(), isNull(), eq(3)))
                .thenReturn(List.of(newest, middle, older));

        var page = service.messages(me, conversation, 2, null);

        assertThat(page.items()).extracting(DirectMessageDtos.MessageResponse::content)
                .containsExactly("中间", "最新");
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(base + "|" + middle.id());
    }

    @Test
    void invalidCursorIsRejected() {
        when(store.find(conversation)).thenReturn(Optional.of(row()));
        assertThatThrownBy(() -> service.messages(me, conversation, 30, "not-a-cursor"))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("VALIDATION_ERROR");
    }
}
