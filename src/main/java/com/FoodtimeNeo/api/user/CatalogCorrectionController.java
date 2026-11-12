package com.FoodtimeNeo.api.user;

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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/catalog-corrections")
@SecurityRequirement(name = "sessionCookie")
public class CatalogCorrectionController {
    private final CatalogCorrectionService corrections;

    public CatalogCorrectionController(CatalogCorrectionService corrections) {
        this.corrections = corrections;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "提交目录纠错提案", description = "仅认证用户；每 UTC+8 自然日最多5单，每单最多3张本人上传的证据；整单审核")
    public ApiResponse<CatalogCorrectionDtos.Response> submit(
            @Valid @RequestBody CatalogCorrectionDtos.CreateRequest body, HttpServletRequest request) {
        return ApiResponse.success(corrections.submit(actor(request), body));
    }

    @GetMapping
    @Operation(summary = "我的纠错提案", description = "仅返回本人提案，最新在前")
    public ApiResponse<PageResponse<CatalogCorrectionDtos.Response>> mine(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(corrections.mine(actor(request), new PageRequest(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "我的纠错提案详情", description = "仅本人可读，包含证据引用和审核结果")
    public ApiResponse<CatalogCorrectionDtos.Response> detail(@PathVariable UUID id, HttpServletRequest request) {
        return ApiResponse.success(corrections.mine(actor(request), id));
    }

    private static UserProfile actor(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
