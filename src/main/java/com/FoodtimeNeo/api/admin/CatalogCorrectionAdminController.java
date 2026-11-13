package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.correction.CatalogCorrectionDtos;
import com.FoodtimeNeo.correction.CatalogCorrectionService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog-corrections")
@SecurityRequirement(name = "sessionCookie")
public class CatalogCorrectionAdminController {
    private final CatalogCorrectionService corrections;

    public CatalogCorrectionAdminController(CatalogCorrectionService corrections) {
        this.corrections = corrections;
    }

    @GetMapping
    @Operation(summary = "纠错审核队列", description = "需要 correction.review 能力；默认仅超管")
    public ApiResponse<PageResponse<CatalogCorrectionDtos.AdminResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return ApiResponse.success(corrections.adminList(actor(request), status, targetType,
                new PageRequest(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "纠错审核详情", description = "包含原值、建议值、证据与提交人；需要 correction.review")
    public ApiResponse<CatalogCorrectionDtos.AdminResponse> detail(@PathVariable UUID id,
            HttpServletRequest request) {
        return ApiResponse.success(corrections.adminDetail(actor(request), id));
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "整单通过或拒绝纠错", description = "需要 correction.review；expectedRevision 与目录基线同时校验，冲突时不覆盖新资料")
    public ApiResponse<CatalogCorrectionDtos.AdminResponse> decide(@PathVariable UUID id,
            @Valid @RequestBody CatalogCorrectionDtos.DecisionRequest body, HttpServletRequest request) {
        return ApiResponse.success(corrections.decide(actor(request), id, body));
    }

    private static UserProfile actor(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
