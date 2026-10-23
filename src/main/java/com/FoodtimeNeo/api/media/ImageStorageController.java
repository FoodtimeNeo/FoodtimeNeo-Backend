package com.FoodtimeNeo.api.media;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.media.service.UploadRateLimiter;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "sessionCookie")
public class ImageStorageController {
    private final ImageStorageService images;
    private final UploadRateLimiter limiter;
    public ImageStorageController(ImageStorageService images, UploadRateLimiter limiter) {
        this.images = images; this.limiter = limiter;
    }

    @PostMapping(value = "/uploads/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "上传图片文件", description = "multipart字段file，JPEG/PNG，不超过10MB；只保存私有暂存文件，后续投稿需使用返回的imageUrl")
    public ApiResponse<ImageStorageService.UploadedImage> upload(@RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        UserProfile user = current(request);
        limiter.check(user, request.getRemoteAddr());
        return ApiResponse.success(images.upload(file, user));
    }

    @GetMapping("/media/{imageId}")
    @Operation(summary = "读取授权图片", description = "作者/管理员可预览暂存图片，其他用户只能读取已审核且父级启用的公开图片；size=thumb 返回480px预览；菜品等公开目录图片长期缓存，社区图片和暂存预览不缓存，每次校验当前可见性")
    public ResponseEntity<byte[]> read(@PathVariable UUID imageId,
                                       @RequestParam(defaultValue = "full") String size,
                                       HttpServletRequest request) {
        if (!size.equals("full") && !size.equals("thumb")) {
            throw new BusinessException("VALIDATION_ERROR", "size只能为full或thumb", HttpStatus.BAD_REQUEST);
        }
        var image = images.read(imageId, current(request), size.equals("thumb"));
        CacheControl cache = image.cacheable()
                ? CacheControl.maxAge(365, java.util.concurrent.TimeUnit.DAYS).cachePublic().immutable()
                : CacheControl.noStore();
        String etag = "\"" + imageId + (size.equals("thumb") ? "-thumb" : "") + "\"";
        if (etag.equals(request.getHeader(HttpHeaders.IF_NONE_MATCH))) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(cache).build();
        }
        var response = ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .eTag(etag).lastModified(image.lastModified())
                .header("X-Content-Type-Options", "nosniff");
        response.cacheControl(cache);
        return response.body(image.bytes());
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
