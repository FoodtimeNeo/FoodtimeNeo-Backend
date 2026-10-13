package com.FoodtimeNeo.moderation.service;

import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.moderation.dto.*;
import com.FoodtimeNeo.moderation.mapper.ModerationStore;
import com.FoodtimeNeo.review.mapper.PublishedReviewMapper;
import com.FoodtimeNeo.submission.mapper.CatalogSubmissionMapper;
import com.FoodtimeNeo.user.entity.UserProfile;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@Validated
public class ModerationService {
    private final ModerationStore store;
    private final PublishedReviewMapper reviews;
    private final CatalogSubmissionMapper catalog;
    public ModerationService(ModerationStore store, PublishedReviewMapper reviews, CatalogSubmissionMapper catalog) {
        this.store = store; this.reviews = reviews; this.catalog = catalog;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<ModerationItem> queue(UserProfile actor, String status, PageRequest page) {
        guard(actor);
        if (!Set.of("pending","approved","rejected").contains(status)) throw bad("审核状态不合法");
        return PageResponse.of(store.page(status,page.size(),page.offset()),store.count(status),page);
    }

    @Transactional
    public ModerationResult moderate(UserProfile actor, String type, UUID id, @Valid ModerationRequest request) {
        guard(actor);
        if (!Set.of("review","dish","stall").contains(type)) throw bad("审核类型不合法");
        ModerationItem item = store.lock(type,id);
        if (item == null) throw new BusinessException("SUBMISSION_NOT_FOUND", "提交不存在", HttpStatus.NOT_FOUND);
        if (!item.status().equals("pending")) throw new BusinessException("ALREADY_REVIEWED", "此提交已审核", HttpStatus.CONFLICT);
        String reason = request.reason() == null ? null : request.reason().strip();
        if (request.action().equals("reject") && (reason == null || reason.isBlank())) throw bad("请填写驳回原因");
        UUID approved = null;
        BigDecimal price = item.price();
        if (request.action().equals("approve")) {
            boolean visible = switch (type) {
                case "review" -> reviews.lockVisibleDish(item.targetId()) != null;
                case "dish" -> catalog.lockStall(item.targetId()) != null;
                default -> catalog.lockHall(item.targetId()) != null;
            };
            if (!visible) throw new BusinessException("TARGET_UNAVAILABLE", "目标已停用，请先恢复", HttpStatus.CONFLICT);
            try {
                if (type.equals("review")) approved = store.publishReview(item);
                else if (type.equals("dish")) {
                    price = request.price() == null ? item.price() : request.price();
                    if (price == null) throw bad("审核通过前请补齐菜品价格");
                    approved = store.publishDish(item,price);
                } else approved = store.publishStall(item);
            } catch (DuplicateKeyException conflict) {
                throw new BusinessException("CATALOG_CONFLICT", "同一食堂已有同名档口", HttpStatus.CONFLICT);
            }
        }
        String status = request.action().equals("approve") ? "approved" : "rejected";
        store.complete(item,actor.id(),status,reason,approved,price);
        store.log(item,actor.id(),request.action(),reason);
        return new ModerationResult(id,status,approved);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<ModerationLogResponse> logs(UserProfile actor, PageRequest page) {
        guard(actor); return PageResponse.of(store.logs(page.size(),page.offset()),store.countLogs(),page);
    }

    private void guard(UserProfile actor) {
        if (!actor.status().equals("active") || !(actor.role().equals("admin") || actor.role().equals("superadmin"))) {
            throw new BusinessException("ACCESS_DENIED", "需要管理员权限", HttpStatus.FORBIDDEN);
        }
    }
    private BusinessException bad(String message) { return new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST); }
}
