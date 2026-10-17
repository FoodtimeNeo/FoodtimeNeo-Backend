package com.FoodtimeNeo.api.media;
import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.media.dto.*;
import com.FoodtimeNeo.media.service.ImageSubmissionService;
import com.FoodtimeNeo.submission.service.SubmissionRateLimiter;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name="sessionCookie")
public class ImageSubmissionController {
    private final ImageSubmissionService images;
    private final SubmissionRateLimiter limiter;
    public ImageSubmissionController(ImageSubmissionService images, SubmissionRateLimiter limiter) { this.images=images; this.limiter=limiter; }
    @PostMapping("/dishes/{dishId}/image-submissions") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="提交本人图片进入审核",description="需先上传文件；不信任客户端上传者、状态或审核字段")
    public ApiResponse<ImageSubmissionResponse> submit(@PathVariable UUID dishId,@Valid @RequestBody ImageSubmissionRequest body,HttpServletRequest request) {
        UserProfile user = current(request);
        limiter.check(user.id(), request.getRemoteAddr());
        return ApiResponse.success(images.submit(user,dishId,body));
    }
    @GetMapping("/users/me/image-submissions") @Operation(summary="当前用户的图片投稿记录",description="包含待审核、通过、驳回，以及已停用菜品的历史和审核原因")
    public ApiResponse<PageResponse<ImageSubmissionResponse>> mine(@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        return ApiResponse.success(images.mine(current(request),new PageRequest(page,size)));
    }
    private UserProfile current(HttpServletRequest request) { return (UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER); }
}
