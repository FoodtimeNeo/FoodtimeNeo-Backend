package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.admin.dto.*;
import com.FoodtimeNeo.admin.service.CatalogImageService;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog/{type}/{id}/images")
@SecurityRequirement(name="sessionCookie")
public class CatalogImageController {
    private static final Logger LOG=LoggerFactory.getLogger(CatalogImageController.class);
    private final CatalogImageService images;
    public CatalogImageController(CatalogImageService images) { this.images=images; }
    @GetMapping @Operation(summary="平台管理员查看食堂封面、档口图集或菜品图片")
    public ApiResponse<PageResponse<CatalogImageResponse>> list(@PathVariable String type,@PathVariable UUID id,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        return ApiResponse.success(images.list(current(request),type,id,new PageRequest(page,size)));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Operation(summary="平台管理员直接添加图片；食堂替换封面")
    public ApiResponse<CatalogImageResponse> add(@PathVariable String type,@PathVariable UUID id,
            @Valid @RequestBody AddCatalogImageRequest body,HttpServletRequest request) {
        UserProfile actor=current(request);
        var result=images.add(actor,type,id,body.imageUrl());
        LOG.info("Catalog image added actor={} type={} target={} image={}",actor.id(),type,id,result.id());
        return ApiResponse.success(result);
    }
    @DeleteMapping("/{imageId}") @Operation(summary="平台管理员移除目录图片；不删除其他位置使用的文件")
    public ApiResponse<Void> remove(@PathVariable String type,@PathVariable UUID id,@PathVariable UUID imageId,HttpServletRequest request) {
        UserProfile actor=current(request); images.remove(actor,type,id,imageId);
        LOG.info("Catalog image removed actor={} type={} target={} image={}",actor.id(),type,id,imageId);
        return ApiResponse.success(null);
    }
    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
