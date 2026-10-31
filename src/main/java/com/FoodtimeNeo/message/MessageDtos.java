package com.FoodtimeNeo.message;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** 个人消息 DTO：时间统一用 Instant。 */
public final class MessageDtos {
    private MessageDtos() { }

    @Schema(description = "个人消息条目")
    public record MessageResponse(
            UUID id,
            String title,
            String body,
            String category,
            String refType,
            UUID refId,
            boolean isRead,
            Instant createdAt) { }

    @Schema(description = "未读消息计数")
    public record UnreadCountResponse(long unreadCount) { }
}
