package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.dto.DishDetailResponse;
import com.FoodtimeNeo.dish.mapper.DishDetailMapper;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;

@Service
@Validated
public class DishDetailService {
    private static final Logger LOG = LoggerFactory.getLogger(DishDetailService.class);
    private final DishDetailMapper dishes;
    public DishDetailService(DishDetailMapper dishes) { this.dishes = dishes; }

    public DishDetailResponse get(@NotNull UUID dishId) {
        try {
            var dish = dishes.findVisible(dishId);
            if (dish == null) {
                throw new BusinessException("DISH_NOT_FOUND", "菜品不存在或暂不可用", HttpStatus.NOT_FOUND);
            }
            return DishDetailResponse.from(dish);
        } catch (DataAccessException exception) {
            LOG.error("Dish detail query failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("DISH_DETAIL_UNAVAILABLE", "菜品详情服务暂时不可用，请稍后重试",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
