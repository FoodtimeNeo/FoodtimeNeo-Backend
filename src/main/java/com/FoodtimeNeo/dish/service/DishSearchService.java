package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.catalog.cache.CatalogQuery;
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
import java.util.Objects;
import java.util.UUID;

@Service
public class DishSearchService {
    public static final int MAX_DINING_HALL_IDS = 50;
    private static final BigDecimal MAX_RATING = BigDecimal.valueOf(5);
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");
    private final DishSearchMapper dishes;

    public DishSearchService(DishSearchMapper dishes) {
        this.dishes = dishes;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    @CatalogQuery
    public PageResponse<DishSearchResponse> search(String query, List<UUID> diningHallIds,
                                                   BigDecimal minPrice, BigDecimal maxPrice,
                                                   BigDecimal minRating, String sort, PageRequest page) {
        DishSearchQuery spec = normalize(query, diningHallIds, minPrice, maxPrice, minRating, sort);
        return PageResponse.of(dishes.search(spec, page.size(), page.offset()), dishes.count(spec), page);
    }

    /** Shared validation; grouped search must obey the same catalog filter contract. */
    public static DishSearchQuery normalize(String query, List<UUID> diningHallIds,
            BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minRating, String sort) {
        String keyword = query == null ? "" : query.strip();
        if (keyword.length() > 50) {
            throw new BusinessException("VALIDATION_ERROR", "菜品关键词最多50个字符", HttpStatus.BAD_REQUEST);
        }
        if (minPrice != null && (minPrice.signum() < 0 || minPrice.compareTo(MAX_PRICE) > 0 || minPrice.stripTrailingZeros().scale() > 2)
                || maxPrice != null && (maxPrice.signum() < 0 || maxPrice.compareTo(MAX_PRICE) > 0 || maxPrice.stripTrailingZeros().scale() > 2)) {
            throw new BusinessException("VALIDATION_ERROR", "价格必须为非负数、最多两位小数且不超过8位整数", HttpStatus.BAD_REQUEST);
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException("VALIDATION_ERROR", "最低价格不能高于最高价格", HttpStatus.BAD_REQUEST);
        }
        if (minRating != null && (minRating.signum() < 0 || minRating.compareTo(MAX_RATING) > 0)) {
            throw new BusinessException("VALIDATION_ERROR", "最低评分须在0到5之间", HttpStatus.BAD_REQUEST);
        }
        List<UUID> halls = List.of();
        if (diningHallIds != null) {
            if (diningHallIds.stream().anyMatch(Objects::isNull)) {
                throw new BusinessException("VALIDATION_ERROR", "食堂筛选参数无效", HttpStatus.BAD_REQUEST);
            }
            halls = diningHallIds.stream().distinct().toList();
            if (halls.size() > MAX_DINING_HALL_IDS) {
                throw new BusinessException("VALIDATION_ERROR", "一次最多筛选50个食堂", HttpStatus.BAD_REQUEST);
            }
        }
        DishSearchSort order = DishSearchSort.fromWire(sort).orElseThrow(() ->
                new BusinessException("VALIDATION_ERROR", "排序方式不合法", HttpStatus.BAD_REQUEST));
        return new DishSearchQuery(keyword, halls,
                minPrice, maxPrice, minRating, order.wireName());
    }
}
