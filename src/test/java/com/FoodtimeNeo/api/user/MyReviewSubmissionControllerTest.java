package com.FoodtimeNeo.api.user;

import com.FoodtimeNeo.auth.security.SessionAuthenticationService;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.GlobalExceptionHandler;
import com.FoodtimeNeo.review.service.ReviewSubmissionService;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyReviewSubmissionControllerTest {
    private final UserProfile current = new UserProfile(UUID.randomUUID(), "1@bjtu.edu.cn", "测试", "user", "active", Instant.EPOCH);

    @Test
    void usesVerifiedCurrentUserDespiteQueryOverride() throws Exception {
        var service = mock(ReviewSubmissionService.class);
        when(service.list(current.id(), new PageRequest(1, 20))).thenReturn(PageResponse.of(List.of(), 0, new PageRequest(1, 20)));
        MockMvcBuilders.standaloneSetup(new MyReviewSubmissionController(service)).build()
                .perform(get("/api/v1/users/me/review-submissions?userId=" + UUID.randomUUID())
                        .requestAttr(SessionAuthenticationService.CURRENT_USER, current)).andExpect(status().isOk());
        verify(service).list(current.id(), new PageRequest(1, 20));
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=0", "page=-1", "size=0", "size=101", "page=nope", "size=2.2", "page=2147483648"})
    void invalidPagingFailsBeforeQuerying(String query) throws Exception {
        var service = mock(ReviewSubmissionService.class);
        MockMvcBuilders.standaloneSetup(new MyReviewSubmissionController(service)).setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/api/v1/users/me/review-submissions?" + query)
                        .requestAttr(SessionAuthenticationService.CURRENT_USER, current)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
