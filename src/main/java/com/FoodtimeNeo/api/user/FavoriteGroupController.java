package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.collection.FavoriteGroupDtos;
import com.FoodtimeNeo.collection.FavoriteGroupService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/favorite-groups")
@SecurityRequirement(name = "sessionCookie")
public class FavoriteGroupController {
    private final FavoriteGroupService groups;

    public FavoriteGroupController(FavoriteGroupService groups) {
        this.groups = groups;
    }

    @GetMapping
    @Operation(summary = "收藏分组列表", description = "按类型分别列出；每类最多20个，按手动顺序排列")
    public ApiResponse<List<FavoriteGroupDtos.Response>> list(
            @RequestParam String kind, HttpServletRequest request) {
        return ApiResponse.success(groups.list(actor(request), kind));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteGroupDtos.Response> create(
            @Valid @RequestBody FavoriteGroupDtos.CreateRequest body, HttpServletRequest request) {
        return ApiResponse.success(groups.create(actor(request), body));
    }

    @PutMapping("/{id}")
    public ApiResponse<FavoriteGroupDtos.Response> update(@PathVariable UUID id,
            @Valid @RequestBody FavoriteGroupDtos.UpdateRequest body, HttpServletRequest request) {
        return ApiResponse.success(groups.update(actor(request), id, body));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, HttpServletRequest request) {
        groups.delete(actor(request), id);
    }

    @GetMapping("/memberships")
    public ApiResponse<FavoriteGroupDtos.MembershipResponse> membership(
            @RequestParam String kind, @RequestParam UUID targetId, HttpServletRequest request) {
        return ApiResponse.success(groups.membership(actor(request), kind, targetId));
    }

    @PutMapping("/{id}/members/{targetId}")
    public ApiResponse<FavoriteGroupDtos.MembershipResponse> addMember(@PathVariable UUID id,
            @PathVariable UUID targetId, HttpServletRequest request) {
        return ApiResponse.success(groups.addMember(actor(request), id, targetId));
    }

    @DeleteMapping("/{id}/members/{targetId}")
    public ApiResponse<FavoriteGroupDtos.MembershipResponse> removeMember(@PathVariable UUID id,
            @PathVariable UUID targetId, HttpServletRequest request) {
        return ApiResponse.success(groups.removeMember(actor(request), id, targetId));
    }

    private UUID actor(HttpServletRequest request) {
        return ((UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER)).id();
    }
}
