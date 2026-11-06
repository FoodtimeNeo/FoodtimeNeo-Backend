package com.FoodtimeNeo.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 一对一私信的请求与响应模型。 */
public final class DirectMessageDtos {
    private DirectMessageDtos() { }

    @Schema(description = "会话中的对方用户")
    public record Participant(UUID id, String displayName,
                              @Schema(nullable = true) String avatarUrl) { }

    @Schema(description = "私信会话摘要")
    public record ConversationResponse(UUID id, Participant other,
                                       @Schema(nullable = true) String lastMessage,
                                       @Schema(nullable = true) UUID lastSenderId,
                                       @Schema(nullable = true) Instant lastMessageAt,
                                       long unreadCount, Instant updatedAt) { }

    @Schema(description = "私信正文")
    public record MessageResponse(UUID id, UUID conversationId, UUID senderId,
                                  String content, Instant createdAt) { }

    @Schema(description = "按时间倒序取一页后转成正序返回，nextCursor 指向更早消息")
    public record MessagePage(List<MessageResponse> items, boolean hasMore,
                              @Schema(nullable = true) String nextCursor) { }

    public record StartConversationRequest(
            @NotNull(message = "请选择私信对象") UUID userId) { }

    public record SendMessageRequest(
            @NotBlank(message = "消息不能为空")
            @Size(max = 1000, message = "消息不能超过1000字") String content,
            @NotNull(message = "请求编号不能为空") UUID clientRequestId) { }

    public record UnreadCountResponse(long unreadCount) { }
}
