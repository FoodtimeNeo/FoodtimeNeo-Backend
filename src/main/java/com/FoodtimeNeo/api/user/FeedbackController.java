package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.auth.security.DeviceIdentityService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.feedback.*;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/feedback")
public class FeedbackController {
    private final FeedbackService service;
    private final DeviceIdentityService devices;
    public FeedbackController(FeedbackService service,DeviceIdentityService devices) { this.service=service;this.devices=devices; }
    @PostMapping
    public ApiResponse<FeedbackDtos.Row> create(@Valid @RequestBody FeedbackDtos.Create input,HttpServletRequest request,HttpServletResponse response) {
        return ApiResponse.success(service.create(actor(request),input,devices.resolve(request,response)));
    }
    @GetMapping
    public ApiResponse<PageResponse<FeedbackDtos.Row>> list(@RequestParam(defaultValue="false") boolean archived,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        return ApiResponse.success(service.list(actor(request),false,archived,new PageRequest(page,size)));
    }
    @GetMapping("/{id}")
    public ApiResponse<FeedbackDtos.Row> detail(@PathVariable UUID id,HttpServletRequest request) {
        return ApiResponse.success(service.detail(actor(request),false,id));
    }
    @GetMapping("/{id}/replies")
    public ApiResponse<PageResponse<FeedbackDtos.Reply>> replies(@PathVariable UUID id,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        return ApiResponse.success(service.replies(actor(request),false,id,new PageRequest(page,size)));
    }
    static UserProfile actor(HttpServletRequest request) {
        return (UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
