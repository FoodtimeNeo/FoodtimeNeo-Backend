package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.admin.dto.*;
import com.FoodtimeNeo.admin.service.UserAdminService;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@SecurityRequirement(name="sessionCookie")
public class UserAdminController {
    private static final Logger LOG = LoggerFactory.getLogger(UserAdminController.class);
    private final UserAdminService users;
    public UserAdminController(UserAdminService users) { this.users = users; }
    @GetMapping @Operation(summary="平台管理员查看用户",description="邮箱或昵称搜索，评论数为正式评论计数；不返回认证信息")
    public ApiResponse<PageResponse<AdminUserResponse>> list(@RequestParam(required=false) String q,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        return ApiResponse.success(users.list(current(request),q,new PageRequest(page,size)));
    }
    @PutMapping("/{id}") @Operation(summary="平台管理员修改他人角色或状态",description="不能改自己的权限；不能修改密码或身份信息；服务端逐次读取最新角色和状态")
    public ApiResponse<Void> update(@PathVariable UUID id,@Valid @RequestBody UserAccessRequest body,HttpServletRequest request) {
        var actor = current(request);
        users.update(actor,id,body);
        LOG.info("User access change actor={} target={} role={} status={}",actor.id(),id,body.role(),body.status());
        return ApiResponse.success(null);
    }
    private UserProfile current(HttpServletRequest request) { return (UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER); }
}
