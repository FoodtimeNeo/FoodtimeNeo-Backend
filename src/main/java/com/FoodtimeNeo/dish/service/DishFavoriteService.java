package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.dto.DishSearchResponse;
import com.FoodtimeNeo.dish.mapper.DishFavoriteMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class DishFavoriteService {
    public record State(boolean saved) {}

    private final DishFavoriteMapper favorites;

    public DishFavoriteService(DishFavoriteMapper favorites) {
        this.favorites = favorites;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResponse<DishSearchResponse> list(UUID user, UUID groupId, boolean unfiled, PageRequest page) {
        return PageResponse.of(favorites.list(user, groupId, unfiled, page.size(), page.offset()),
                favorites.count(user, groupId, unfiled), page);
    }

    public State state(UUID user, UUID dish) {
        return new State(favorites.saved(user, dish));
    }

    @Transactional
    public State save(UUID user, UUID dish) {
        if (favorites.save(user, dish) == null) {
            throw new BusinessException("DISH_NOT_FOUND", "菜品不存在或暂不可用", HttpStatus.NOT_FOUND);
        }
        return new State(true);
    }

    public State remove(UUID user, UUID dish) {
        favorites.remove(user, dish);
        return new State(false);
    }
}
