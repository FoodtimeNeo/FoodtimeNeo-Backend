package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.ai.dto.AiModelMapResponse;
import com.FoodtimeNeo.ai.dto.UpdateAiModelMappingRequest;
import com.FoodtimeNeo.ai.service.AiModelRouter;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 模型映射管理：查看各逻辑槽位当前生效的模型 ID（Redis 覆盖优先，否则 env 默认），
 * 超级管理员可热切换，无需重启。monitor 进程晋升新模型走同一个 POST 入口。
 */
@RestController
@RequestMapping("/api/v1/admin/ai/models")
@SecurityRequirement(name = "sessionCookie")
public class AiModelAdminController {
    private final AiModelRouter router;

    public AiModelAdminController(AiModelRouter router) {
        this.router = router;
    }

    @GetMapping
    @Operation(summary = "查看 AI 模型映射", description = "每个槽位当前生效的模型 ID 及其来源（redis 覆盖或 env 默认）")
    public ApiResponse<AiModelMapResponse> snapshot() {
        List<AiModelMapResponse.SlotEntry> slots = new ArrayList<>();
        router.snapshot().forEach((slot, row) ->
                slots.add(new AiModelMapResponse.SlotEntry(slot, row.get("model"), row.get("source"))));
        return ApiResponse.success(new AiModelMapResponse(slots, router.meta("pinned-at"),
                router.meta("pinned-by"), router.meta("reason")));
    }

    @PostMapping
    @Operation(summary = "热切换模型映射", description = "仅平台管理员；写入 Redis 后下一次 AI 调用即生效，无需重启")
    public ApiResponse<Void> pin(@Valid @RequestBody UpdateAiModelMappingRequest body, HttpServletRequest request) {
        router.pin(current(request), body.slot(), body.model(), body.reason());
        return ApiResponse.success(null);
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
