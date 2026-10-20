package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.admin.dto.*;
import com.FoodtimeNeo.admin.service.CatalogAdminService;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.review.dto.PublishedReviewResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "sessionCookie")
public class CatalogAdminController {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogAdminController.class);
    private final CatalogAdminService catalog;
    public CatalogAdminController(CatalogAdminService catalog) { this.catalog = catalog; }
    @GetMapping("/catalog/{type}") @Operation(summary = "管理员目录列表",description = "包括启用与停用记录；type为dining-halls/stalls/dishes")
    public ApiResponse<PageResponse<AdminCatalogResponse>> list(@PathVariable String type,@RequestParam(required = false) UUID parentId,
            @RequestParam(defaultValue = "1") int page,@RequestParam(defaultValue = "20") int size,HttpServletRequest request) {
        return ApiResponse.success(catalog.list(current(request),type,parentId,new PageRequest(page,size)));
    }
    @GetMapping("/catalog/{type}/{id}") @Operation(summary = "管理员目录详情")
    public ApiResponse<AdminCatalogResponse> get(@PathVariable String type,@PathVariable UUID id,HttpServletRequest request) { return ApiResponse.success(catalog.get(current(request),type,id)); }
    @PostMapping("/catalog/{type}") @ResponseStatus(HttpStatus.CREATED) @Operation(summary = "新增正式食堂档口或菜品",description = "服务端生成UUID，管理员可维护；菜品必须有价格")
    public ApiResponse<AdminCatalogResponse> create(@PathVariable String type,@Valid @RequestBody CatalogEditRequest body,HttpServletRequest request) { return ApiResponse.success(catalog.save(current(request),type,null,body)); }
    @PutMapping("/catalog/{type}/{id}") @Operation(summary = "编辑名称简介价格或启停状态",description = "已有所属关系保持不变，无楼层字段")
    public ApiResponse<AdminCatalogResponse> update(@PathVariable String type,@PathVariable UUID id,@Valid @RequestBody CatalogEditRequest body,HttpServletRequest request) { return ApiResponse.success(catalog.save(current(request),type,id,body)); }
    @GetMapping("/reviews") @Operation(summary = "管理员查看正式评论",description = "包括停用菜品历史，仅管理员可见")
    public ApiResponse<PageResponse<PublishedReviewResponse>> reviews(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,@RequestParam(defaultValue = "20") int size,HttpServletRequest request) {
        return ApiResponse.success(catalog.reviews(current(request),q,new PageRequest(page,size)));
    }
    @DeleteMapping("/reviews/{reviewId}") @Operation(summary = "移除违规正式评论",description = "软删除并保留审核历史；评分在同一事务内更新")
    public ApiResponse<Void> remove(@PathVariable UUID reviewId,@Valid @RequestBody RemoveReviewRequest body,HttpServletRequest request) {
        UserProfile actor = current(request);
        catalog.removeReview(actor,reviewId);
        // The service transaction has committed. Operational audit does not invent a DB delete action.
        LOG.info("Review removal actor={} target={} reason={}",actor.id(),reviewId,body.reason().replace('\n',' ').replace('\r',' '));
        return ApiResponse.success(null);
    }
    @DeleteMapping("/catalog/{type}/{id}") @Operation(summary = "删除无关联目录记录",description = "数据库RESTRICT外键阻止删除仍被引用的记录；409时可改为停用")
    public ApiResponse<Void> removeCatalog(@PathVariable String type,@PathVariable UUID id,HttpServletRequest request) {
        UserProfile actor = current(request); catalog.remove(actor,type,id);
        LOG.info("Catalog removal actor={} type={} target={}",actor.id(),type,id);
        return ApiResponse.success(null);
    }
    private UserProfile current(HttpServletRequest request) { return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER); }
}
