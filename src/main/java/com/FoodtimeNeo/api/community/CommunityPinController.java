package com.FoodtimeNeo.api.community;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.community.pin.CommunityPinService;
import com.FoodtimeNeo.community.pin.PinDtos;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name="sessionCookie")
public class CommunityPinController {
    private final CommunityPinService pins;
    public CommunityPinController(CommunityPinService pins) { this.pins=pins; }
    private UserProfile actor(HttpServletRequest request) {return (UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER);}
    @GetMapping("/community/pins")
    public ApiResponse<PinDtos.Snapshot> visible(HttpServletRequest request) {return ApiResponse.success(pins.read(actor(request),false));}
    @GetMapping("/admin/community/pins")
    public ApiResponse<PinDtos.Snapshot> manage(HttpServletRequest request) {return ApiResponse.success(pins.read(actor(request),true));}
    @PutMapping("/admin/community/pins/order")
    public ApiResponse<PinDtos.Snapshot> order(@Valid @RequestBody PinDtos.Order input,HttpServletRequest request) {return ApiResponse.success(pins.order(actor(request),input));}
    @PutMapping("/admin/community/pins/{id}")
    public ApiResponse<PinDtos.Snapshot> pin(@PathVariable UUID id,@Valid @RequestBody PinDtos.Change input,HttpServletRequest request) {return ApiResponse.success(pins.pin(actor(request),id,input.expectedRevision()));}
    @DeleteMapping("/admin/community/pins/{id}")
    public ApiResponse<PinDtos.Snapshot> unpin(@PathVariable UUID id,@RequestParam long expectedRevision,HttpServletRequest request) {return ApiResponse.success(pins.unpin(actor(request),id,expectedRevision));}
}
