package com.FoodtimeNeo.review.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.review.dto.*;
import com.FoodtimeNeo.review.mapper.PublishedReviewMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;

@Service
@Validated
public class PublishedReviewService {
    private final PublishedReviewMapper reviews;
    public PublishedReviewService(PublishedReviewMapper reviews) { this.reviews = reviews; }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public PageResponse<PublishedReviewResponse> list(@NotNull UUID dishId, @NotNull PageRequest page) {
        visible(dishId);
        return PageResponse.of(reviews.visiblePage(dishId, page.size(), page.offset()), reviews.countForDish(dishId), page);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<PublishedReviewResponse> mine(@NotNull UUID userId, @NotNull PageRequest page) {
        return PageResponse.of(reviews.ownedPage(userId, page.size(), page.offset()), reviews.countOwned(userId), page);
    }

    @Transactional
    public ReviewCreatedResponse submit(@NotNull UUID dishId, @NotNull UUID userId,
                                        @NotNull @Valid SubmitReviewRequest request) {
        visible(dishId);
        UUID id = UUID.randomUUID();
        if (reviews.submit(id, dishId, userId, request.rating(), request.content().strip()) == 0) {
            throw new BusinessException("REVIEW_PENDING", "此菜品已有待审核评论", HttpStatus.CONFLICT);
        }
        return reviews.findSubmission(id);
    }

    @Transactional
    public void delete(@NotNull UUID id, @NotNull UUID userId) {
        if (reviews.deleteOwned(id, userId) == 0) {
            throw new BusinessException("REVIEW_NOT_FOUND", "评论不存在", HttpStatus.NOT_FOUND);
        }
    }

    @Transactional
    public DishRatingResponse rating(@NotNull UUID dishId) {
        visible(dishId);
        return reviews.rating(dishId);
    }

    private void visible(UUID id) {
        if (reviews.lockVisibleDish(id) == null) {
            throw new BusinessException("DISH_NOT_FOUND", "菜品不存在或暂不可用", HttpStatus.NOT_FOUND);
        }
    }
}
