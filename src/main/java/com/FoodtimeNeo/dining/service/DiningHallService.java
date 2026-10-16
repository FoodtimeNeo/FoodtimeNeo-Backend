package com.FoodtimeNeo.dining.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dining.dto.DiningHallResponse;
import com.FoodtimeNeo.dining.mapper.DiningHallMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DiningHallService {
    private static final Logger LOG = LoggerFactory.getLogger(DiningHallService.class);
    private final DiningHallMapper halls;

    public DiningHallService(DiningHallMapper halls) { this.halls = halls; }

    public List<DiningHallResponse> list() {
        try {
            return halls.findActive().stream().map(DiningHallResponse::from).toList();
        } catch (DataAccessException exception) {
            LOG.error("Dining hall query failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("DINING_HALLS_UNAVAILABLE", "食堂列表服务暂时不可用，请稍后重试",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
