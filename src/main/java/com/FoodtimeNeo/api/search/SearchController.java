package com.FoodtimeNeo.api.search;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.search.*;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** POST keeps transient device coordinates out of URLs and persistent public catalog caches. */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final CatalogSearchService catalog;
    private final NoteSearchService notes;
    private final HallDistanceProvider locations;
    public SearchController(CatalogSearchService catalog, NoteSearchService notes, HallDistanceProvider locations) {
        this.catalog = catalog; this.notes = notes; this.locations = locations;
    }
    @GetMapping("/capabilities")
    public ApiResponse<Map<String,Object>> capabilities() {
        return ApiResponse.success(Map.of("distanceHallIds", locations.hallIds(), "coordinateSystem", "WGS84", "distanceKind", "straight-line"));
    }
    @PostMapping("/stalls/query")
    public ApiResponse<PageResponse<CatalogSearchService.Stall>> stalls(@RequestBody SearchRequest request) {
        return ApiResponse.success(catalog.search(request));
    }
    @GetMapping("/stalls")
    @com.FoodtimeNeo.catalog.cache.PublicCatalog
    public ApiResponse<PageResponse<CatalogSearchService.Stall>> publicStalls(@ModelAttribute SearchRequest request) {
        publicQuery(request);
        return ApiResponse.success(catalog.search(request));
    }
    @GetMapping("/dishes")
    @com.FoodtimeNeo.catalog.cache.PublicCatalog
    public ApiResponse<PageResponse<CatalogSearchService.Dish>> publicDishes(@ModelAttribute SearchRequest request) {
        publicQuery(request);
        return ApiResponse.success(catalog.dishes(request));
    }
    private static void publicQuery(SearchRequest request) {
        if (request.latitude() != null || request.longitude() != null || "distance".equals(request.sort()))
            throw new com.FoodtimeNeo.common.exception.BusinessException("VALIDATION_ERROR",
                    "定位查询请使用临时请求", org.springframework.http.HttpStatus.BAD_REQUEST);
    }
    @PostMapping("/dishes/query")
    public ApiResponse<PageResponse<CatalogSearchService.Dish>> dishes(@RequestBody SearchRequest request) {
        return ApiResponse.success(catalog.dishes(request));
    }
    @PostMapping("/notes/query")
    public ApiResponse<PageResponse<NoteSearchService.Note>> notes(@RequestBody SearchRequest body, HttpServletRequest request) {
        var user = (UserProfile) request.getAttribute(SessionAuthenticationService.CURRENT_USER);
        return ApiResponse.success(notes.search(user.id(), body));
    }
}
