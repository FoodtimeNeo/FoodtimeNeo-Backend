package com.FoodtimeNeo.dining.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dining.dto.StallImageResponse;
import com.FoodtimeNeo.dining.dto.StallResponse;
import com.FoodtimeNeo.dining.entity.StallImage;
import com.FoodtimeNeo.dining.mapper.StallImageMapper;
import com.FoodtimeNeo.dining.mapper.StallMapper;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
public class StallService {
    private static final Logger LOG = LoggerFactory.getLogger(StallService.class);
    private final StallMapper stalls;
    private final StallImageMapper images;
    public StallService(StallMapper stalls, StallImageMapper images) {
        this.stalls = stalls;
        this.images = images;
    }

    public List<StallResponse> listByDiningHall(@NotNull UUID diningHallId) {
        try {
            var rows = stalls.findActiveByDiningHall(diningHallId);
            if (rows.isEmpty()) {
                throw new BusinessException("DINING_HALL_NOT_FOUND", "食堂不存在或暂不可用", HttpStatus.NOT_FOUND);
            }
            var imagesByStall = groupImages(images.findActiveByDiningHall(diningHallId));
            // The left join retains the parent when it has no active children; do not expose that empty row.
            return rows.stream().filter(row -> row.id() != null)
                    .map(row -> StallResponse.from(row, imagesByStall.getOrDefault(row.id(), List.of())))
                    .toList();
        } catch (DataAccessException exception) {
            LOG.error("Stall list query failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("STALLS_UNAVAILABLE", "档口列表服务暂时不可用，请稍后重试",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public StallResponse getById(@NotNull UUID stallId) {
        try {
            var row = stalls.findVisibleById(stallId);
            if (row == null) {
                throw new BusinessException("STALL_NOT_FOUND", "档口不存在或暂不可用", HttpStatus.NOT_FOUND);
            }
            // The mapper already returns sort_order ASC, id ASC; keep that order for direct carousel use.
            var gallery = images.findByStall(stallId).stream()
                    .filter(image -> image.id() != null)
                    .map(StallImageResponse::from).toList();
            return StallResponse.from(row, gallery);
        } catch (DataAccessException exception) {
            LOG.error("Stall detail query failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("STALLS_UNAVAILABLE", "档口服务暂时不可用，请稍后重试",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static Map<UUID, List<StallImageResponse>> groupImages(List<StallImage> rows) {
        return rows.stream().filter(image -> image.id() != null && image.stallId() != null)
                .collect(Collectors.groupingBy(StallImage::stallId, LinkedHashMap::new,
                        Collectors.mapping(StallImageResponse::from, Collectors.toList())));
    }
}
