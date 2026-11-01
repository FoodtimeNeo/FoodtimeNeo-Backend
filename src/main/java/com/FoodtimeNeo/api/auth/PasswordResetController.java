package com.FoodtimeNeo.api.auth;

import com.FoodtimeNeo.auth.dto.*;
import com.FoodtimeNeo.auth.reset.PasswordResetService;
import com.FoodtimeNeo.auth.security.DeviceIdentityService;
import com.FoodtimeNeo.common.api.ApiResponse;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth/password-reset")
public class PasswordResetController {
    private final PasswordResetService resets;private final DeviceIdentityService devices;
    public PasswordResetController(PasswordResetService resets,DeviceIdentityService devices){this.resets=resets;this.devices=devices;}
    @PostMapping(value="/email-code",consumes=MediaType.APPLICATION_JSON_VALUE) @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<EmailCodeResponse> send(@Valid @RequestBody PasswordResetEmailRequest body,HttpServletRequest request,HttpServletResponse response) {
        return ApiResponse.success(resets.send(body,request.getRemoteAddr(),devices.resolve(request,response)));
    }
    @PostMapping(value="/verify",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<PasswordResetGrantResponse> verify(@Valid @RequestBody PasswordResetVerifyRequest body,HttpServletRequest request,HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        return ApiResponse.success(resets.verify(body,request.getRemoteAddr()));
    }
    @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Void> reset(@Valid @RequestBody PasswordResetRequest body,HttpServletRequest request) {
        resets.reset(body,request.getRemoteAddr());return ApiResponse.success(null);
    }
}
