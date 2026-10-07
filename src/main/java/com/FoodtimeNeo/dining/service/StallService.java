package com.FoodtimeNeo.dining.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dining.dto.StallResponse;
import com.FoodtimeNeo.dining.mapper.StallMapper;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.UUID;

@Service
@Validated
public class StallService {
    private static final Logger LOG = LoggerFactory.getLogger(StallService.class);
    private final StallMapper stalls;
    public StallService(StallMapper stalls) { this.stalls = stalls; }

    public List<StallResponse> listByDiningHall(@NotNull UUID diningHallId) {
        try {
            var rows = stalls.findActiveByDiningHall(diningHallId);
            if (rows.isEmpty()) {
                throw new BusinessException("DINING_HALL_NOT_FOUND", "食堂不存在或暂不可用", HttpStatus.NOT_FOUND);
            }
            // The left join retains the parent when it has no active children; do not expose that empty row.
            return rows.stream().filter(row -> row.id() != null).map(StallResponse::from).toList();
        } catch (DataAccessException exception) {
            LOG.error("Stall list query failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("STALLS_UNAVAILABLE", "档口列表服务暂时不可用，请稍后重试",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
