package com.FoodtimeNeo.auth.controller;

import com.FoodtimeNeo.auth.dto.RegisterRequest;
import com.FoodtimeNeo.auth.dto.RegisterResponse;
import com.FoodtimeNeo.auth.service.RegistrationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "账号注册")
public class RegistrationController {
    private final RegistrationService registrations;

    public RegistrationController(RegistrationService registrations) {
        this.registrations = registrations;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "注册用户", description = "使用数字前缀的@bjtu.edu.cn邮箱注册，默认昵称为干饭人加6位随机数字，角色为user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "注册成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求格式或参数校验失败"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "邮箱已注册"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "注册服务暂时不可用")
    })
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(registrations.register(request));
    }
}
