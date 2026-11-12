package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.notification.NotificationDtos;
import com.FoodtimeNeo.notification.NotificationService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
@SecurityRequirement(name = "sessionCookie")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/notification-preferences")
    @Operation(summary = "七类提醒开关", description = "站内消息始终可读；开关只影响 Android 系统提醒。新类型默认关闭")
    public ApiResponse<NotificationDtos.Preferences> preferences(HttpServletRequest request) {
        return ApiResponse.success(notifications.preferences(actor(request).id()));
    }

    @PutMapping("/notification-preferences/{category}")
    @Operation(summary = "更新一类提醒开关", description = "按 revision 条件更新；开启前的事件不补发")
    public ApiResponse<NotificationDtos.Preference> update(@PathVariable String category,
            @Valid @RequestBody NotificationDtos.PreferenceRequest body, HttpServletRequest request) {
        return ApiResponse.success(notifications.update(actor(request).id(), category, body));
    }

    @GetMapping("/notification-events")
    @Operation(summary = "按游标读取提醒事件", description = "稳定 eventId/seq；afterSeq 之后的本人事件，供 Android 去重投递")
    public ApiResponse<NotificationDtos.EventPage> events(HttpServletRequest request,
            @RequestParam(defaultValue = "0") long afterSeq,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(notifications.events(actor(request).id(), afterSeq, limit));
    }

    @PostMapping("/notification-events/{eventId}/delivered")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "标记 Android 通知已投递", description = "幂等；不改变站内消息已读状态或事件入箱时间")
    public void delivered(@PathVariable UUID eventId, HttpServletRequest request) {
        notifications.delivered(actor(request).id(), eventId);
    }

    private static UserProfile actor(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
