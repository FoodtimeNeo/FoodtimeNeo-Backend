package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.ApiResponse;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.dish.dto.DishSearchResponse;
import com.FoodtimeNeo.dish.service.DishFavoriteService;
import com.FoodtimeNeo.user.entity.UserProfile;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/dish-favorites")
@SecurityRequirement(name = "sessionCookie")
public class DishFavoriteController {
    private final DishFavoriteService favorites;

    public DishFavoriteController(DishFavoriteService favorites) {
        this.favorites = favorites;
    }

    @GetMapping
    public ApiResponse<PageResponse<DishSearchResponse>> list(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(favorites.list(actor(request), new PageRequest(page, size)));
    }

    @GetMapping("/{dish}")
    public ApiResponse<DishFavoriteService.State> state(HttpServletRequest request, @PathVariable UUID dish) {
        return ApiResponse.success(favorites.state(actor(request), dish));
    }

    @PutMapping("/{dish}")
    public ApiResponse<DishFavoriteService.State> save(HttpServletRequest request, @PathVariable UUID dish) {
        return ApiResponse.success(favorites.save(actor(request), dish));
    }

    @DeleteMapping("/{dish}")
    public ApiResponse<DishFavoriteService.State> remove(HttpServletRequest request, @PathVariable UUID dish) {
        return ApiResponse.success(favorites.remove(actor(request), dish));
    }

    private UUID actor(HttpServletRequest request) {
        return ((UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER)).id();
    }
}
