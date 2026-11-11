package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.lifecycle.AccountLifecycleDtos.*;
import com.FoodtimeNeo.user.lifecycle.AccountLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/account-lifecycle")
public class AccountLifecycleController {
    private final AccountLifecycleService lifecycle;private final SessionAuthenticationService sessions;
    public AccountLifecycleController(AccountLifecycleService lifecycle,SessionAuthenticationService sessions){this.lifecycle=lifecycle;this.sessions=sessions;}
    private UserProfile actor(HttpServletRequest request){return (UserProfile)request.getAttribute(SessionAuthenticationService.CURRENT_USER);}
    @GetMapping public ApiResponse<Summary> summary(HttpServletRequest request){return ApiResponse.success(lifecycle.summary(actor(request)));}
    @PostMapping("/deletion-code") @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Sent> deletionCode(HttpServletRequest request){return ApiResponse.success(lifecycle.send(actor(request),"delete",request.getRemoteAddr()));}
    @PostMapping("/unbind-code") @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Sent> unbindCode(HttpServletRequest request){return ApiResponse.success(lifecycle.send(actor(request),"unbind",request.getRemoteAddr()));}
    @PostMapping("/unbind") public ApiResponse<Unbound> unbind(@Valid @RequestBody Code body,HttpServletRequest request){return ApiResponse.success((Unbound)lifecycle.confirm(actor(request),"unbind",body,request.getRemoteAddr()));}
    @PostMapping("/delete") public ApiResponse<Deleted> delete(@Valid @RequestBody Code body,HttpServletRequest request){
        var result=(Deleted)lifecycle.confirm(actor(request),"delete",body,request.getRemoteAddr());sessions.clear(request);return ApiResponse.success(result);
    }
}
