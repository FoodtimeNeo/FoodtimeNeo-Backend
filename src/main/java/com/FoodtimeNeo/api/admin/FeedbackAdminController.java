package com.FoodtimeNeo.api.admin;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.feedback.*;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/feedback")
public class FeedbackAdminController {
    private final FeedbackService service;
    public FeedbackAdminController(FeedbackService service) { this.service=service; }
    @GetMapping
    public ApiResponse<PageResponse<FeedbackDtos.Row>> list(@RequestParam(defaultValue="false") boolean archived,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        return ApiResponse.success(service.list(actor(request),true,archived,new PageRequest(page,size)));
    }
    @GetMapping("/{id}")
    public ApiResponse<FeedbackDtos.Row> detail(@PathVariable UUID id,HttpServletRequest request) {
        return ApiResponse.success(service.detail(actor(request),true,id));
    }
    @GetMapping("/{id}/replies")
    public ApiResponse<PageResponse<FeedbackDtos.Reply>> replies(@PathVariable UUID id,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request) {
        return ApiResponse.success(service.replies(actor(request),true,id,new PageRequest(page,size)));
    }
    @PutMapping("/{id}")
    public ApiResponse<FeedbackDtos.Row> update(@PathVariable UUID id,@Valid @RequestBody FeedbackDtos.Update input,HttpServletRequest request) {
        return ApiResponse.success(service.update(actor(request),id,input));
    }
    @PostMapping("/export")
    public ApiResponse<FeedbackDtos.Export> export(HttpServletRequest request) {
        return ApiResponse.success(service.export(actor(request)));
    }
    @PostMapping("/import")
    public ApiResponse<FeedbackDtos.ImportResult> importCsv(@Valid @RequestBody FeedbackDtos.Import input,HttpServletRequest request) {
        return ApiResponse.success(service.importCsv(actor(request),input));
    }
    private static UserProfile actor(HttpServletRequest request) {
        return (UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER);
    }
}
