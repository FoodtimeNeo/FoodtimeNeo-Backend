package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.invitation.dto.*;
import com.FoodtimeNeo.invitation.service.InvitationService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/invitation-codes")
@SecurityRequirement(name = "sessionCookie")
public class InvitationAdminController {
    private static final Logger LOG = LoggerFactory.getLogger(InvitationAdminController.class);
    private final InvitationService invitations;
    public InvitationAdminController(InvitationService invitations) { this.invitations = invitations; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "平台管理员创建邀请码", description = "明文码只在本次响应中返回一次，服务端只存摘要；注册时可选填写，用于记录邀请归因")
    public ApiResponse<CreatedInvitationResponse> generate(@Valid @RequestBody CreateInvitationRequest body,
                                                           HttpServletRequest request) {
        var actor = current(request);
        CreatedInvitationResponse created = invitations.generate(actor, body);
        LOG.info("Invitation code created actor={} code={}", actor.id(), created.id());
        return ApiResponse.success(created);
    }

    @GetMapping
    @Operation(summary = "平台管理员查看邀请码", description = "按状态筛选，不返回明文码")
    public ApiResponse<PageResponse<InvitationCodeResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ApiResponse.success(invitations.list(current(request), status, new PageRequest(page, size)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "平台管理员启用或停用邀请码", description = "停用后新注册不再接受，已用邀请码注册的账号不受影响")
    public ApiResponse<Void> setStatus(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateInvitationStatusRequest body,
                                       HttpServletRequest request) {
        var actor = current(request);
        invitations.setStatus(actor, id, body.status());
        LOG.info("Invitation code status change actor={} code={} status={}", actor.id(), id, body.status());
        return ApiResponse.success(null);
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
