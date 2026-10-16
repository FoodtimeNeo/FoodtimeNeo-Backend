package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.dto.DishImageResponse;
import com.FoodtimeNeo.dish.mapper.DishImageMapper;
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
public class DishImageService {
    private static final Logger LOG = LoggerFactory.getLogger(DishImageService.class);
    private final DishImageMapper images;
    public DishImageService(DishImageMapper images) { this.images = images; }

    public PageResponse<DishImageResponse> list(@NotNull UUID dishId, @NotNull PageRequest page) {
        try {
            var rows = images.findVisiblePage(dishId, page.size(), page.offset());
            if (rows.isEmpty()) {
                throw new BusinessException("DISH_NOT_FOUND", "菜品不存在或暂不可用", HttpStatus.NOT_FOUND);
            }
            var items = rows.stream().filter(row -> row.id() != null).map(DishImageResponse::from).toList();
            return PageResponse.of(items, rows.get(0).total(), page);
        } catch (DataAccessException exception) {
            LOG.error("Dish image query failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("DISH_IMAGES_UNAVAILABLE", "菜品图册服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
