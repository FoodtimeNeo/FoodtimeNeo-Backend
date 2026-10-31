package com.FoodtimeNeo.message;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;

/**
 * 个人消息服务：仅操作当前用户自己的消息。
 */
@Service
@Validated
public class MessageService {
    private final MessageStore store;
    public MessageService(MessageStore store) { this.store = store; }

    @Transactional(readOnly = true)
    public PageResponse<MessageDtos.MessageResponse> list(UUID userId, PageRequest page) {
        return PageResponse.of(store.page(userId, page.size(), page.offset()), store.count(userId), page);
    }

    @Transactional(readOnly = true)
    public MessageDtos.UnreadCountResponse unreadCount(UUID userId) {
        return new MessageDtos.UnreadCountResponse(store.unreadCount(userId));
    }

    @Transactional
    public void markRead(UUID userId, UUID id) {
        if (!store.owned(id, userId)) throw new BusinessException("MESSAGE_NOT_FOUND", "消息不存在", HttpStatus.NOT_FOUND);
        store.markRead(id, userId);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        store.markAllRead(userId);
    }
}
