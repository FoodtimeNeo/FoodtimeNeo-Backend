package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.dto.DishSearchResponse;
import com.FoodtimeNeo.dish.mapper.DishSearchMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DishSearchService {
    private final DishSearchMapper dishes;

    public DishSearchService(DishSearchMapper dishes) {
        this.dishes = dishes;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<DishSearchResponse> search(String query, PageRequest page) {
        String keyword = query.strip();
        if (keyword.length() > 50) {
            throw new BusinessException("VALIDATION_ERROR", "菜品关键词最多50个字符", HttpStatus.BAD_REQUEST);
        }
        if (keyword.isEmpty()) return PageResponse.of(List.of(), 0, page);
        return PageResponse.of(dishes.search(keyword, page.size(), page.offset()), dishes.count(keyword), page);
    }
}
