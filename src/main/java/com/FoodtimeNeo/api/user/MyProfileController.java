package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.user.dto.ProfileResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
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
}
