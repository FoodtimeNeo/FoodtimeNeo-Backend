package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.safety.SafetyService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "sessionCookie")
public class SafetyController {
    private final SafetyService safety;

    public SafetyController(SafetyService safety) {
        this.safety = safety;
    }

    @GetMapping("/users/me/blocks")
    public ApiResponse<PageResponse<SafetyService.BlockedUser>> blockedUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        return ApiResponse.success(safety.blockedUsers(actor(request), new PageRequest(page, size)));
    }

    @GetMapping("/users/{id}/block")
    public ApiResponse<SafetyService.BlockState> state(@PathVariable UUID id, HttpServletRequest request) {
        return ApiResponse.success(safety.blockState(actor(request), id));
    }

    @PutMapping("/users/{id}/block")
    public ApiResponse<SafetyService.BlockState> block(@PathVariable UUID id, HttpServletRequest request) {
        return ApiResponse.success(safety.setBlock(actor(request), id, true));
    }

    @DeleteMapping("/users/{id}/block")
    public ApiResponse<SafetyService.BlockState> unblock(@PathVariable UUID id, HttpServletRequest request) {
        return ApiResponse.success(safety.setBlock(actor(request), id, false));
    }

    public record ReportRequest(@NotBlank @Pattern(regexp = "post|comment|message") String targetType,
                                @NotNull UUID targetId,
                                @NotBlank @Size(max = 500) String reason) {}

    @PostMapping("/reports")
    public ApiResponse<Void> report(@Valid @RequestBody ReportRequest report, HttpServletRequest request) {
        safety.report(actor(request), report.targetType(), report.targetId(), report.reason());
        return ApiResponse.success(null);
    }

    private UUID actor(HttpServletRequest request) {
        return ((UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER)).id();
    }
}
