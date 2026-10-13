package com.FoodtimeNeo.review.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.review.dto.ReviewSubmissionResponse;
import com.FoodtimeNeo.review.mapper.ReviewSubmissionMapper;
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
public class ReviewSubmissionService {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewSubmissionService.class);
    private final ReviewSubmissionMapper submissions;
    public ReviewSubmissionService(ReviewSubmissionMapper submissions) { this.submissions = submissions; }

    public PageResponse<ReviewSubmissionResponse> list(@NotNull UUID userId, @NotNull PageRequest page) {
        try {
            var rows = submissions.findOwnedPage(userId, page.size(), page.offset());
            var items = rows.stream().filter(row -> row.id() != null).map(ReviewSubmissionResponse::from).toList();
            return PageResponse.of(items, rows.get(0).total(), page);
        } catch (DataAccessException exception) {
            LOG.error("Review submission query failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException("REVIEW_SUBMISSIONS_UNAVAILABLE", "评论审核记录服务暂时不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
