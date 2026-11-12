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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DishSearchService {
    private static final BigDecimal MAX_RATING = BigDecimal.valueOf(5);
    private final DishSearchMapper dishes;

    public DishSearchService(DishSearchMapper dishes) {
        this.dishes = dishes;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<DishSearchResponse> search(String query, List<UUID> diningHallIds,
                                                   BigDecimal minPrice, BigDecimal maxPrice,
                                                   BigDecimal minRating, String sort, PageRequest page) {
        String keyword = query == null ? "" : query.strip();
        if (keyword.length() > 50) {
            throw new BusinessException("VALIDATION_ERROR", "菜品关键词最多50个字符", HttpStatus.BAD_REQUEST);
        }
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw new BusinessException("VALIDATION_ERROR", "价格不能为负数", HttpStatus.BAD_REQUEST);
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException("VALIDATION_ERROR", "最低价格不能高于最高价格", HttpStatus.BAD_REQUEST);
        }
        if (minRating != null && (minRating.signum() < 0 || minRating.compareTo(MAX_RATING) > 0)) {
            throw new BusinessException("VALIDATION_ERROR", "最低评分须在0到5之间", HttpStatus.BAD_REQUEST);
        }
        DishSearchSort order = DishSearchSort.fromWire(sort).orElseThrow(() ->
                new BusinessException("VALIDATION_ERROR", "排序方式不合法", HttpStatus.BAD_REQUEST));
        DishSearchQuery spec = new DishSearchQuery(keyword,
                diningHallIds == null ? List.of() : diningHallIds.stream().distinct().toList(),
                minPrice, maxPrice, minRating, order.wireName());
        return PageResponse.of(dishes.search(spec, page.size(), page.offset()), dishes.count(spec), page);
    }
}
