package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.safety.SafetyService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reports")
@SecurityRequirement(name = "sessionCookie")
public class ReportsAdminController {
    private final SafetyService safety;

    public ReportsAdminController(SafetyService safety) {
        this.safety = safety;
    }

    @GetMapping
    public ApiResponse<PageResponse<SafetyService.ReportRow>> pending(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(safety.pending(new PageRequest(page, size)));
    }

    @PutMapping("/{id}/review")
    public ApiResponse<Void> review(@PathVariable UUID id, HttpServletRequest request) {
        UserProfile user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        safety.review(id, user.id());
        return ApiResponse.success(null);
    }
}
