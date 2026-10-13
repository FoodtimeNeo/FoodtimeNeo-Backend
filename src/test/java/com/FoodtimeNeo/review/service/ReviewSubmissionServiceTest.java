package com.FoodtimeNeo.review.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.review.mapper.ReviewSubmissionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewSubmissionServiceTest {
    @Test
    void databaseFailureReturns503WithoutInternalDetails() {
        var mapper = mock(ReviewSubmissionMapper.class);
        UUID id = UUID.randomUUID();
        when(mapper.findOwnedPage(id, 20, 0)).thenThrow(new DataAccessResourceFailureException("private-sql"));
        assertThatThrownBy(() -> new ReviewSubmissionService(mapper).list(id, new PageRequest(1, 20)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("REVIEW_SUBMISSIONS_UNAVAILABLE");
                    assertThat(e.getStatus().value()).isEqualTo(503);
                }).hasMessageNotContaining("private-sql");
    }
}
