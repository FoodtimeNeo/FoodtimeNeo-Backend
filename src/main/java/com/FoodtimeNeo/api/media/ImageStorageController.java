package com.FoodtimeNeo.api.media;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.media.service.ImageStorageService;
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
    public ImageStorageController(ImageStorageService images) { this.images = images; }

    @PostMapping(value = "/uploads/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "上传图片文件", description = "multipart字段file，JPEG/PNG，不超过10MB；只保存私有暂存文件，后续投稿需使用返回的imageUrl")
    public ApiResponse<ImageStorageService.UploadedImage> upload(@RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        return ApiResponse.success(images.upload(file, current(request)));
    }

    @GetMapping("/media/{imageId}")
    @Operation(summary = "读取授权图片", description = "作者/管理员可预览暂存图片，其他用户只能读取已审核且父级启用的公开图片")
    public ResponseEntity<byte[]> read(@PathVariable UUID imageId, HttpServletRequest request) {
        var image = images.read(imageId, current(request));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff").body(image.bytes());
    }

    private UserProfile current(HttpServletRequest request) {
        return (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
