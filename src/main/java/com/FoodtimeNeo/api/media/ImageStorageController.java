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
    @Operation(summary = "读取授权图片", description = "size=thumb返回480px预览；目录图片长期缓存，公开社区图片和头像采用private,no-cache，每次复用先校验权限；暂存及待审预览no-store")
    public ResponseEntity<byte[]> read(@PathVariable UUID imageId,
                                       @RequestParam(defaultValue = "full") String size,
                                       HttpServletRequest request) {
        if (!size.equals("full") && !size.equals("thumb")) {
            throw new BusinessException("VALIDATION_ERROR", "size只能为full或thumb", HttpStatus.BAD_REQUEST);
        }
        var image = images.inspect(imageId, current(request));
        CacheControl cache = switch (image.policy()) {
            case PUBLIC_IMMUTABLE -> CacheControl.maxAge(365, java.util.concurrent.TimeUnit.DAYS).cachePublic().immutable();
            case PRIVATE_REVALIDATE -> CacheControl.noCache().cachePrivate();
            case NO_STORE -> CacheControl.noStore();
        };
        String etag = "\"" + imageId + (size.equals("thumb") ? "-thumb" : "") + "\"";
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(cache);
        headers.setETag(etag);
        headers.setLastModified(image.lastModified());
        headers.set("X-Content-Type-Options", "nosniff");
        if (image.policy() == ImageStorageService.CachePolicy.PRIVATE_REVALIDATE) headers.setVary(java.util.List.of("Cookie"));
        String condition = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        boolean unchanged = condition != null && java.util.Arrays.stream(condition.split(","))
                .map(String::trim).map(value -> value.startsWith("W/") ? value.substring(2) : value)
                .anyMatch(value -> value.equals("*") || value.equals(etag));
        if (image.policy() != ImageStorageService.CachePolicy.NO_STORE && unchanged) {
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_MODIFIED);
        }
        headers.setContentType(MediaType.parseMediaType(image.contentType()));
        return new ResponseEntity<>(images.readBytes(image, size.equals("thumb")), headers, HttpStatus.OK);
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
