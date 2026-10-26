package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.message.MessageDtos;
import com.FoodtimeNeo.message.MessageService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/messages")
@SecurityRequirement(name = "sessionCookie")
public class MeMessageController {
    private final MessageService messages;
    public MeMessageController(MessageService messages) { this.messages = messages; }

    @GetMapping
    @Operation(summary = "个人消息分页列表", description = "按创建时间倒序，含is_read字段")
    public ApiResponse<PageResponse<MessageDtos.MessageResponse>> list(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(messages.list(user.id(), new PageRequest(page, size)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读消息计数", description = "小红点用；读完单条或全部后重新拉取")
    public ApiResponse<MessageDtos.UnreadCountResponse> unreadCount(HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(messages.unreadCount(user.id()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记单条消息已读", description = "幂等；只能操作自己的消息")
    public ApiResponse<Void> markRead(@PathVariable UUID id, HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        messages.markRead(user.id(), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部消息标为已读", description = "幂等；仅影响当前用户")
    public ApiResponse<Void> markAllRead(HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        messages.markAllRead(user.id());
        return ApiResponse.success(null);
    }
}
