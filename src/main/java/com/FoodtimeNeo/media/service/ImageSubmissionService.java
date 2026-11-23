package com.FoodtimeNeo.media.service;
import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.media.dto.*;
import com.FoodtimeNeo.media.mapper.ImageSubmissionStore;
import com.FoodtimeNeo.ai.service.SubmissionDiagnoseEvent;
import com.FoodtimeNeo.review.mapper.PublishedReviewMapper;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.UUID;

@Service
public class ImageSubmissionService {
    private final ImageSubmissionStore submissions;
    private final ImageStorageService images;
    private final PublishedReviewMapper catalog;
    private final ApplicationEventPublisher events;
    public ImageSubmissionService(ImageSubmissionStore submissions,ImageStorageService images,PublishedReviewMapper catalog,
                                  ApplicationEventPublisher events) {
        this.submissions=submissions;this.images=images;this.catalog=catalog;this.events=events;
    }
    @Transactional
    public ImageSubmissionResponse submit(UserProfile actor,UUID dish,ImageSubmissionRequest body) {
        if(catalog.lockVisibleDish(dish)==null) throw new BusinessException("DISH_NOT_FOUND","菜品不存在或已停用",HttpStatus.NOT_FOUND);
        images.requireOwnedReference(body.imageUrl(),actor.id());
        if(body.reviewSubmissionId()!=null) {
            if(!submissions.ownsReview(body.reviewSubmissionId(),dish,actor.id()))
                throw new BusinessException("REVIEW_NOT_FOUND","点评不存在",HttpStatus.NOT_FOUND);
            var existing=submissions.forReview(body.reviewSubmissionId());
            if(existing.isPresent()) {
                if(existing.get().imageUrl().equals(body.imageUrl())) return existing.get();
                throw new BusinessException("REVIEW_PHOTO_EXISTS","该点评已有图片",HttpStatus.CONFLICT);
            }
        }
        var created = submissions.create(dish,actor.id(),body.imageUrl(),body.reviewSubmissionId());
        events.publishEvent(new SubmissionDiagnoseEvent("photo", created.id()));
        return created;
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ,readOnly=true)
    public PageResponse<ImageSubmissionResponse> mine(UserProfile actor,PageRequest page) {
        return PageResponse.of(submissions.page(actor.id(),page.size(),page.offset()),submissions.count(actor.id()),page);
    }
}
