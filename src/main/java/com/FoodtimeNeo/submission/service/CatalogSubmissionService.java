package com.FoodtimeNeo.submission.service;

import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.submission.dto.*;
import com.FoodtimeNeo.submission.mapper.CatalogSubmissionMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.UUID;

@Service
@Validated
public class CatalogSubmissionService {
    private final CatalogSubmissionMapper submissions;
    private final ImageStorageService images;
    public CatalogSubmissionService(CatalogSubmissionMapper submissions, ImageStorageService images) {
        this.submissions = submissions; this.images = images;
    }

    @Transactional
    public SubmissionCreatedResponse dish(UUID stallId, UUID userId, @Valid SubmitDishRequest body) {
        if (submissions.lockStall(stallId) == null) throw new BusinessException("STALL_NOT_FOUND", "档口不存在或暂不可用", HttpStatus.NOT_FOUND);
        images.requireOwnedReference(body.imageUrl(), userId);
        UUID id = UUID.randomUUID();
        submissions.submitDish(id, stallId, userId, new SubmitDishRequest(body.name().strip(), body.description(), body.price(), body.imageUrl()));
        return submissions.createdDish(id);
    }

    @Transactional
    public SubmissionCreatedResponse stall(UUID hallId, UUID userId, @Valid SubmitStallRequest body) {
        if (submissions.lockHall(hallId) == null) throw new BusinessException("DINING_HALL_NOT_FOUND", "食堂不存在或暂不可用", HttpStatus.NOT_FOUND);
        images.requireOwnedReference(body.coverImageUrl(), userId);
        UUID id = UUID.randomUUID();
        submissions.submitStall(id, hallId, userId, new SubmitStallRequest(body.name().strip(), body.description(), body.coverImageUrl()));
        return submissions.createdStall(id);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<StallSubmissionResponse> stalls(UUID userId, PageRequest page) {
        return PageResponse.of(submissions.ownedStalls(userId, page.size(), page.offset()), submissions.countStalls(userId), page);
    }
}
