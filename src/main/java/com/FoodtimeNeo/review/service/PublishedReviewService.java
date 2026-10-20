package com.FoodtimeNeo.review.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.review.dto.*;
import com.FoodtimeNeo.review.mapper.PublishedReviewMapper;
import com.FoodtimeNeo.ai.service.SubmissionDiagnoseEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher events;
    public PublishedReviewService(PublishedReviewMapper reviews, ApplicationEventPublisher events) {
        this.reviews = reviews; this.events = events;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<PublishedReviewResponse> list(@NotNull UUID dishId, @NotNull PageRequest page) {
        readable(dishId);
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
            throw new BusinessException("REVIEW_PENDING", "该菜品已有一条待审核点评，审核完成后可再评", HttpStatus.CONFLICT);
        }
        events.publishEvent(new SubmissionDiagnoseEvent("review", id));
        return reviews.findSubmission(id);
    }

    @Transactional
    public void delete(@NotNull UUID id, @NotNull UUID userId) {
        if (reviews.deleteOwned(id, userId) == 0) {
            throw new BusinessException("REVIEW_NOT_FOUND", "评论不存在", HttpStatus.NOT_FOUND);
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public DishRatingResponse rating(@NotNull UUID dishId) {
        readable(dishId);
        return reviews.rating(dishId);
    }

    private void readable(UUID id) {
        if (reviews.visibleDish(id) == null) {
            throw new BusinessException("DISH_NOT_FOUND", "菜品不存在或暂不可用", HttpStatus.NOT_FOUND);
        }
    }

    private void visible(UUID id) {
        if (reviews.lockVisibleDish(id) == null) {
            throw new BusinessException("DISH_NOT_FOUND", "菜品不存在或暂不可用", HttpStatus.NOT_FOUND);
        }
    }
}
