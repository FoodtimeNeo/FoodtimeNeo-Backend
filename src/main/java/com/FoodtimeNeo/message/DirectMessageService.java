package com.FoodtimeNeo.message;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 一对一私信：会话成员校验、幂等发送与未读计数。 */
@Service
@Validated
public class DirectMessageService {
    private static final int MAX_PAGE_SIZE = 100;
    private final DirectMessageStore store;
    private final DirectMessageRateLimiter limiter;

    public DirectMessageService(DirectMessageStore store, DirectMessageRateLimiter limiter) {
        this.store = store;
        this.limiter = limiter;
    }

    @Transactional
    public DirectMessageDtos.ConversationResponse start(UUID me, UUID other) {
        if (me.equals(other)) throw bad("SELF_CONVERSATION", "不能给自己发私信");
        if (!store.activeUser(other)) throw notFound();
        UUID low = lower(me, other), high = me.equals(low) ? other : me;
        DirectMessageStore.ConversationRow conversation = store.findPair(low, high).orElse(null);
        if (conversation == null) {
            store.insert(UUID.randomUUID(), low, high);
            conversation = store.findPair(low, high).orElseThrow(DirectMessageService::notFound);
        }
        store.markRead(conversation.id(), me);
        return store.summary(conversation.id(), me).orElseThrow(DirectMessageService::notFound);
    }

    @Transactional(readOnly = true)
    public DirectMessageDtos.ConversationResponse detail(UUID me, UUID conversationId) {
        requireVisible(conversationId, me);
        return store.summary(conversationId, me).orElseThrow(DirectMessageService::notFound);
    }

    @Transactional(readOnly = true)
    public PageResponse<DirectMessageDtos.ConversationResponse> list(UUID me, PageRequest page) {
        return PageResponse.of(store.page(me, page.size(), page.offset()), store.count(me), page);
    }

    @Transactional(readOnly = true)
    public DirectMessageDtos.MessagePage messages(UUID me, UUID conversationId, int size, String cursor) {
        DirectMessageStore.ConversationRow conversation = requireVisible(conversationId, me);
        int limit = Math.max(1, Math.min(MAX_PAGE_SIZE, size));
        Cursor before = Cursor.parse(cursor);
        List<DirectMessageDtos.MessageResponse> descending = new ArrayList<>(store.messages(
                conversation.id(), before == null ? null : before.at(), before == null ? null : before.id(),
                limit + 1));
        boolean hasMore = descending.size() > limit;
        if (hasMore) descending.remove(descending.size() - 1);
        String next = hasMore && !descending.isEmpty()
                ? Cursor.of(descending.get(descending.size() - 1)).encode() : null;
        Collections.reverse(descending);
        return new DirectMessageDtos.MessagePage(descending, hasMore, next);
    }

    @Transactional
    public DirectMessageDtos.MessageResponse send(UUID me, UUID conversationId,
                                                  @Valid DirectMessageDtos.SendMessageRequest request,
                                                  String remoteAddress) {
        DirectMessageStore.ConversationRow conversation = requireMember(conversationId, me);
        String content = request.content().strip();
        if (content.isEmpty()) throw bad("VALIDATION_ERROR", "消息不能为空");
        limiter.check(me, remoteAddress);
        int inserted = store.insertMessage(UUID.randomUUID(), conversation.id(), me, content,
                request.clientRequestId());
        if (inserted == 0) {
            DirectMessageDtos.MessageResponse existing = store
                    .findByClientRequest(me, request.clientRequestId())
                    .orElseThrow(() -> new BusinessException("REQUEST_CONFLICT", "请求编号冲突，请重试",
                            HttpStatus.CONFLICT));
            if (!existing.conversationId().equals(conversation.id())) {
                throw new BusinessException("REQUEST_CONFLICT", "请求编号已用于其他会话", HttpStatus.CONFLICT);
            }
            return existing;
        }
        store.revive(conversation.id());
        store.touch(conversation.id());
        return store.findByClientRequest(me, request.clientRequestId()).orElseThrow(DirectMessageService::notFound);
    }

    @Transactional
    public void markRead(UUID me, UUID conversationId) {
        store.markRead(requireMember(conversationId, me).id(), me);
    }

    @Transactional
    public void hide(UUID me, UUID conversationId) {
        if (store.hide(requireMember(conversationId, me).id(), me) == 0) throw notFound();
    }

    @Transactional(readOnly = true)
    public DirectMessageDtos.UnreadCountResponse unreadCount(UUID me) {
        return new DirectMessageDtos.UnreadCountResponse(store.unreadTotal(me));
    }

    private DirectMessageStore.ConversationRow requireMember(UUID conversationId, UUID me) {
        return store.find(conversationId).filter(row -> row.includes(me)).orElseThrow(DirectMessageService::notFound);
    }

    private DirectMessageStore.ConversationRow requireVisible(UUID conversationId, UUID me) {
        DirectMessageStore.ConversationRow row = requireMember(conversationId, me);
        if (row.deletedFor(me)) throw notFound();
        return row;
    }

    private static UUID lower(UUID a, UUID b) {
        return a.toString().compareTo(b.toString()) <= 0 ? a : b;
    }

    private static BusinessException bad(String code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
    }

    private static BusinessException notFound() {
        return new BusinessException("CONVERSATION_NOT_FOUND", "会话不存在或暂不可见", HttpStatus.NOT_FOUND);
    }

    private record Cursor(Instant at, UUID id) {
        static Cursor parse(String raw) {
            if (raw == null || raw.isBlank()) return null;
            int separator = raw.indexOf('|');
            if (separator <= 0) throw bad("VALIDATION_ERROR", "分页游标无效");
            try {
                return new Cursor(Instant.parse(raw.substring(0, separator)),
                        UUID.fromString(raw.substring(separator + 1)));
            } catch (RuntimeException invalid) {
                throw bad("VALIDATION_ERROR", "分页游标无效");
            }
        }
        static Cursor of(DirectMessageDtos.MessageResponse message) {
            return new Cursor(message.createdAt(), message.id());
        }
        String encode() {
            return at + "|" + id;
        }
    }
}
