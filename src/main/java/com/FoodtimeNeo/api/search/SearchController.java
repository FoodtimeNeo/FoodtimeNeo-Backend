package com.FoodtimeNeo.api.search;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.web.EndpointRateLimiter;
import com.FoodtimeNeo.search.*;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;

/** POST keeps transient device coordinates out of URLs and persistent public catalog caches. */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final CatalogSearchService catalog;
    private final NoteSearchService notes;
    private final HallDistanceProvider locations;
    private final EndpointRateLimiter limiter;
    public SearchController(CatalogSearchService catalog, NoteSearchService notes,
                            HallDistanceProvider locations, EndpointRateLimiter limiter) {
        this.catalog = catalog; this.notes = notes; this.locations = locations; this.limiter = limiter;
    }
    @GetMapping("/capabilities")
    @com.FoodtimeNeo.catalog.cache.PublicCatalog
    public ApiResponse<Map<String,Object>> capabilities() {
        return ApiResponse.success(Map.of("distanceHallIds", locations.hallIds(), "coordinateSystem", "WGS84", "distanceKind", "straight-line"));
    }
    @PostMapping("/stalls/query")
    public ApiResponse<PageResponse<CatalogSearchService.Stall>> stalls(@RequestBody SearchRequest request, HttpServletRequest http) {
        limit(http);
        return ApiResponse.success(catalog.search(request));
    }
    @GetMapping("/stalls")
    @com.FoodtimeNeo.catalog.cache.PublicCatalog
    public ApiResponse<PageResponse<CatalogSearchService.Stall>> publicStalls(@ModelAttribute SearchRequest request, HttpServletRequest http) {
        limit(http);
        publicQuery(request);
        return ApiResponse.success(catalog.search(request));
    }
    @GetMapping("/dishes")
    @com.FoodtimeNeo.catalog.cache.PublicCatalog
    public ApiResponse<PageResponse<CatalogSearchService.Dish>> publicDishes(@ModelAttribute SearchRequest request, HttpServletRequest http) {
        limit(http);
        publicQuery(request);
        return ApiResponse.success(catalog.dishes(request));
    }
    private static void publicQuery(SearchRequest request) {
        if (request.latitude() != null || request.longitude() != null || "distance".equals(request.sort()))
            throw new com.FoodtimeNeo.common.exception.BusinessException("VALIDATION_ERROR",
                    "定位查询请使用临时请求", org.springframework.http.HttpStatus.BAD_REQUEST);
    }
    @PostMapping("/dishes/query")
    public ApiResponse<PageResponse<CatalogSearchService.Dish>> dishes(@RequestBody SearchRequest request, HttpServletRequest http) {
        limit(http);
        return ApiResponse.success(catalog.dishes(request));
    }
    @PostMapping("/notes/query")
    public ApiResponse<PageResponse<NoteSearchService.Note>> notes(@RequestBody SearchRequest body, HttpServletRequest request) {
        limit(request);
        var user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(notes.search(user.id(), body));
    }
    private void limit(HttpServletRequest request) {
        var user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        String actor = user == null ? request.getRemoteAddr() : user.id().toString();
        limiter.check("search", actor, request.getRemoteAddr(), 120, 600, Duration.ofMinutes(1));
    }
}
