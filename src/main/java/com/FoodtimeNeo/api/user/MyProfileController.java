package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.user.dto.ProfileResponse;
import com.FoodtimeNeo.user.dto.UpdateProfileRequest;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/profile")
@SecurityRequirement(name = "sessionCookie")
public class MyProfileController {
    private final ProfileService profiles;
    public MyProfileController(ProfileService profiles) { this.profiles = profiles; }
    @GetMapping
    @Operation(summary = "当前用户个人资料", description = "用户ID来自校验后的会话；头像可为空，不允许指定其他用户")
    public ApiResponse<ProfileResponse> current(HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(profiles.current(user.id()));
    }
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "修改当前用户昵称与头像", description = "用户ID来自校验后的会话；昵称必填，头像须为本人上传的同站图片引用，传null清除头像")
    public ApiResponse<ProfileResponse> update(@Valid @RequestBody UpdateProfileRequest body,
                                               HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(profiles.update(user.id(), body));
    }
}
