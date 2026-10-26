package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.notice.NoticeDtos;
import com.FoodtimeNeo.notice.NoticeService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/notices")
@SecurityRequirement(name = "sessionCookie")
public class MeNoticeController {
    private final NoticeService notices;
    public MeNoticeController(NoticeService notices) { this.notices = notices; }

    @GetMapping("/feed")
    @Operation(summary = "公告增量轮询", description = "items为seq大于since_seq的正序；unreadCount为该用户未读公告数；客户端保存最大seq下次传入")
    public ApiResponse<NoticeDtos.NoticeFeedResponse> feed(HttpServletRequest request,
            @RequestParam(defaultValue = "0") long since_seq,
            @RequestParam(defaultValue = "20") int limit) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(notices.feed(user.id(), since_seq, limit));
    }

    @GetMapping
    @Operation(summary = "公告分页列表", description = "按发布时间倒序")
    public ApiResponse<PageResponse<NoticeDtos.NoticeResponse>> list(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(notices.list(user.id(), new PageRequest(page, size)));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记公告已读", description = "幂等；重复调用返回成功")
    public ApiResponse<Void> markRead(@PathVariable UUID id, HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        notices.markRead(user.id(), id);
        return ApiResponse.success(null);
    }
}
