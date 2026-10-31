package com.FoodtimeNeo.api.admin;

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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notices")
@SecurityRequirement(name = "sessionCookie")
public class AdminNoticeController {
    private final NoticeService notices;
    public AdminNoticeController(NoticeService notices) { this.notices = notices; }

    @GetMapping
    @Operation(summary = "公告管理列表", description = "按发布时间倒序分页")
    public ApiResponse<PageResponse<NoticeDtos.NoticeResponse>> list(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(notices.adminList(current(request), new PageRequest(page, size)));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<NoticeDtos.NoticeResponse>> search(HttpServletRequest request,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(defaultValue="") @jakarta.validation.constraints.Size(max=200) String q) {
        return ApiResponse.success(notices.search(current(request),new PageRequest(page,size),q));
    }
    @GetMapping("/settings")
    public ApiResponse<NoticeDtos.SettingsResponse> settings(HttpServletRequest request) { return ApiResponse.success(notices.settings(current(request))); }
    @PutMapping("/settings")
    public ApiResponse<NoticeDtos.SettingsResponse> settings(@Valid @RequestBody NoticeDtos.SettingsRequest body,HttpServletRequest request) {
        return ApiResponse.success(notices.settings(current(request),body));
    }
    @PostMapping("/markdown") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoticeDtos.NoticeResponse> markdown(@Valid @RequestBody NoticeDtos.MarkdownRequest body,HttpServletRequest request) {
        return ApiResponse.success(notices.saveMarkdown(current(request),null,body));
    }
    @PutMapping("/{id}/markdown")
    public ApiResponse<NoticeDtos.NoticeResponse> markdown(@PathVariable UUID id,@Valid @RequestBody NoticeDtos.MarkdownRequest body,HttpServletRequest request) {
        return ApiResponse.success(notices.saveMarkdown(current(request),id,body));
    }
    @GetMapping("/{id}")
    @Operation(summary = "公告详情", description = "管理端查看单条公告")
    public ApiResponse<NoticeDtos.NoticeResponse> get(@PathVariable UUID id, HttpServletRequest request) {
        return ApiResponse.success(notices.adminGet(current(request), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "发布公告", description = "兼容旧客户端；保存后需另行设置为当前公告")
    public ApiResponse<NoticeDtos.NoticeResponse> create(@Valid @RequestBody NoticeDtos.CreateNoticeRequest body,
                                                         HttpServletRequest request) {
        return ApiResponse.success(notices.create(current(request), body));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑公告", description = "标题正文整体替换；不改变seq，已读标记保留")
    public ApiResponse<NoticeDtos.NoticeResponse> update(@PathVariable UUID id,
                                                         @Valid @RequestBody NoticeDtos.UpdateNoticeRequest body,
                                                         HttpServletRequest request) {
        return ApiResponse.success(notices.update(current(request), id, body));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除公告", description = "硬删；关联已读记录级联删除")
    public ApiResponse<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        notices.delete(current(request), id);
        return ApiResponse.success(null);
    }

    private UserProfile current(HttpServletRequest request) { return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER); }
}
