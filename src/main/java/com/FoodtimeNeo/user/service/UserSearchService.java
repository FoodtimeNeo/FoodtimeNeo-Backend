package com.FoodtimeNeo.user.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.dto.UserSearchResponse;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.mapper.UserSearchStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserSearchService {
    private final UserSearchStore store;

    public UserSearchService(UserSearchStore store) {
        this.store = store;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<UserSearchResponse> search(UserProfile viewer, String query, PageRequest page) {
        String keyword = query == null ? "" : query.strip();
        if (keyword.length() > 50) {
            throw new BusinessException("VALIDATION_ERROR", "用户名关键词最多50个字符", HttpStatus.BAD_REQUEST);
        }
        if (keyword.isEmpty()) return PageResponse.of(List.of(), 0, page);
        return PageResponse.of(store.search(viewer.id(), keyword, page), store.count(viewer.id(), keyword), page);
    }
}
