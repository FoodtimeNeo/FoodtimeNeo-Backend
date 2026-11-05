package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.message.DirectMessageDtos;
import com.FoodtimeNeo.message.DirectMessageService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/conversations")
@SecurityRequirement(name = "sessionCookie")
public class DirectMessageController {
    private final DirectMessageService messages;

    public DirectMessageController(DirectMessageService messages) {
        this.messages = messages;
    }

    @GetMapping
    @Operation(summary = "私信会话列表", description = "按最近消息排序；删除的会话不返回，收到新消息自动恢复")
    public ApiResponse<PageResponse<DirectMessageDtos.ConversationResponse>> list(
            HttpServletRequest request, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(messages.list(current(request).id(), new PageRequest(page, size)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "私信未读总数", description = "用于信箱与头部角标")
    public ApiResponse<DirectMessageDtos.UnreadCountResponse> unreadCount(HttpServletRequest request) {
        return ApiResponse.success(messages.unreadCount(current(request).id()));
    }

    @PostMapping
    @Operation(summary = "开始或打开与某用户的会话", description = "幂等；返回会话摘要，并清除本人侧的隐藏状态")
    public ApiResponse<DirectMessageDtos.ConversationResponse> start(
            @Valid @RequestBody DirectMessageDtos.StartConversationRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(messages.start(current(request).id(), body.userId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "会话摘要", description = "当前用户与对方的会话信息")
    public ApiResponse<DirectMessageDtos.ConversationResponse> detail(
            @PathVariable UUID id, HttpServletRequest request) {
        return ApiResponse.success(messages.detail(current(request).id(), id));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "会话消息", description = "默认取最新一页并按时间正序返回；cursor 取更早消息")
    public ApiResponse<DirectMessageDtos.MessagePage> messages(
            @PathVariable UUID id, HttpServletRequest request,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String cursor) {
        return ApiResponse.success(messages.messages(current(request).id(), id, size, cursor));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "发送私信", description = "纯文本，最多1000字；clientRequestId 保证重试幂等")
    public ApiResponse<DirectMessageDtos.MessageResponse> send(
            @PathVariable UUID id, @Valid @RequestBody DirectMessageDtos.SendMessageRequest body,
            HttpServletRequest request) {
        return ApiResponse.success(messages.send(current(request).id(), id, body,
                request.getRemoteAddr()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "会话标为已读", description = "幂等；同时清除本人侧的隐藏状态")
    public ApiResponse<Void> read(@PathVariable UUID id, HttpServletRequest request) {
        messages.markRead(current(request).id(), id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "隐藏会话", description = "仅影响当前用户；对方发送新消息后自动恢复")
    public ApiResponse<Void> hide(@PathVariable UUID id, HttpServletRequest request) {
        messages.hide(current(request).id(), id);
        return ApiResponse.success(null);
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
