package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.history.BrowsingHistoryDtos;
import com.FoodtimeNeo.history.BrowsingHistoryService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/browsing-history")
@SecurityRequirement(name = "sessionCookie")
public class BrowsingHistoryController {
    private final BrowsingHistoryService history;

    public BrowsingHistoryController(BrowsingHistoryService history) {
        this.history = history;
    }

    @GetMapping("/settings")
    @Operation(summary = "浏览历史设置", description = "账号级开关、revision 与两类记录代次；visitor 也可读")
    public ApiResponse<BrowsingHistoryDtos.SettingsResponse> settings(HttpServletRequest request) {
        return ApiResponse.success(history.settings(actor(request)));
    }

    @PutMapping("/settings")
    @Operation(summary = "更新浏览历史开关", description = "按 revision 条件更新；关闭时递增两类 epoch，重新开启不重置")
    public ApiResponse<BrowsingHistoryDtos.SettingsResponse> updateSettings(
            @Valid @RequestBody BrowsingHistoryDtos.SettingsRequest body, HttpServletRequest request) {
        return ApiResponse.success(history.updateSettings(actor(request), body));
    }

    @GetMapping
    @Operation(summary = "分类型浏览历史", description = "帖子/菜品各自最多50条；失效目标只返回占位")
    public ApiResponse<PageResponse<BrowsingHistoryDtos.Item>> list(HttpServletRequest request,
            @RequestParam String kind, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(history.list(actor(request), kind, new PageRequest(page, size)));
    }

    @PutMapping("/{kind}/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "记录一次详情访问", description = "只有成功打开可见详情后调用；epoch 过期时拒绝")
    public void record(@PathVariable String kind, @PathVariable UUID targetId,
            @Valid @RequestBody BrowsingHistoryDtos.RecordRequest body, HttpServletRequest request) {
        history.record(actor(request), kind, targetId, body);
    }

    @DeleteMapping("/{kind}/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除一条浏览历史", description = "删除后递增该类 epoch，旧离线任务不能补回")
    public void deleteOne(@PathVariable String kind, @PathVariable UUID targetId, HttpServletRequest request) {
        history.deleteOne(actor(request), kind, targetId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "清空一类浏览历史", description = "必须显式提供 kind，只清空该类型并递增 epoch")
    public void clear(@RequestParam String kind, HttpServletRequest request) {
        history.clear(actor(request), kind);
    }

    private static UserProfile actor(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
