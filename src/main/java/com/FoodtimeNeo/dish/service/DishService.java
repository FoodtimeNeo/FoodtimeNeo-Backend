package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.dto.DishListItemResponse;
import com.FoodtimeNeo.dish.mapper.DishMapper;
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
public class DishService {
    private static final Logger LOG = LoggerFactory.getLogger(DishService.class);
    private final DishMapper dishes;
    public DishService(DishMapper dishes) { this.dishes = dishes; }

    public List<DishListItemResponse> listByStall(@NotNull UUID stallId) {
        try {
            var rows = dishes.findActiveByStall(stallId);
            if (rows.isEmpty()) {
                throw new BusinessException("STALL_NOT_FOUND", "档口不存在或暂不可用", HttpStatus.NOT_FOUND);
            }
            // Keep the visible parent sentinel out of the public dish list.
            return rows.stream().filter(row -> row.id() != null).map(DishListItemResponse::from).toList();
        } catch (DataAccessException exception) {
            LOG.error("Dish list query failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("DISHES_UNAVAILABLE", "菜品列表服务暂时不可用，请稍后重试",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
