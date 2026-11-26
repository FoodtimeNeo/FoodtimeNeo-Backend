package com.FoodtimeNeo.search;

import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.dto.CommunityPostResponse;
import com.FoodtimeNeo.community.mapper.CommunityStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;

@Service
public class NoteSearchService {
    public record Note(CommunityPostResponse post, Double distanceMeters, String diningHallName) { }
    private final CommunityStore posts;
    private final HallDistanceProvider locations;
    public NoteSearchService(CommunityStore posts, HallDistanceProvider locations) { this.posts = posts; this.locations = locations; }
    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<Note> search(UUID viewer, SearchRequest request) {
        String q = request.q() == null ? "" : request.q().strip();
        String sort = request.sort() == null ? "relevance" : request.sort();
        String type = request.type() == null ? "all" : request.type();
        if (q.length() > 50 || !Set.of("relevance", "latest", "likes", "distance").contains(sort)
                || !Set.of("all", "original", "repost").contains(type))
            throw new BusinessException("VALIDATION_ERROR", "笔记搜索条件不合法", HttpStatus.BAD_REQUEST);
        var distances = locations.distances(request.latitude(), request.longitude());
        if (sort.equals("distance") && request.latitude() == null)
            throw new BusinessException("LOCATION_REQUIRED", "请先获取当前位置", HttpStatus.BAD_REQUEST);
        var page = new PageRequest(request.page() == null ? 1 : request.page(), request.size() == null ? 20 : request.size());
        List<UUID> halls = List.of();
        if (request.diningHallIds() != null) {
            if (request.diningHallIds().stream().anyMatch(Objects::isNull)) {
                throw new BusinessException("VALIDATION_ERROR", "食堂筛选参数无效", HttpStatus.BAD_REQUEST);
            }
            halls = request.diningHallIds().stream().distinct().toList();
            if (halls.size() > com.FoodtimeNeo.dish.service.DishSearchService.MAX_DINING_HALL_IDS) {
                throw new BusinessException("VALIDATION_ERROR", "一次最多筛选50个食堂", HttpStatus.BAD_REQUEST);
            }
        }
        return posts.search(viewer, q, sort, type, halls, distances, page);
    }
}
