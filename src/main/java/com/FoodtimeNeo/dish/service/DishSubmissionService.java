package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.dto.DishSubmissionResponse;
import com.FoodtimeNeo.dish.mapper.DishSubmissionMapper;
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
public class DishSubmissionService {
    private static final Logger LOG = LoggerFactory.getLogger(DishSubmissionService.class);
    private final DishSubmissionMapper submissions;
    public DishSubmissionService(DishSubmissionMapper submissions) { this.submissions = submissions; }

    public PageResponse<DishSubmissionResponse> list(@NotNull UUID userId, @NotNull PageRequest page) {
        try {
            var rows = submissions.findOwnedPage(userId, page.size(), page.offset());
            var items = rows.stream().filter(row -> row.id() != null).map(DishSubmissionResponse::from).toList();
            return PageResponse.of(items, rows.get(0).total(), page);
        } catch (DataAccessException exception) {
            LOG.error("Dish submission query failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("DISH_SUBMISSIONS_UNAVAILABLE", "菜品提交记录服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
