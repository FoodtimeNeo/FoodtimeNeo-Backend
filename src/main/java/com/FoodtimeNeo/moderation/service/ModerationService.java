package com.FoodtimeNeo.moderation.service;

import com.FoodtimeNeo.common.api.*;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.mapper.AiModerationStore;
import com.FoodtimeNeo.config.properties.AiTriageProperties;
import com.FoodtimeNeo.community.mapper.CommunityStore;
import com.FoodtimeNeo.moderation.dto.*;
import com.FoodtimeNeo.moderation.mapper.ModerationStore;
import com.FoodtimeNeo.review.service.ReviewAccessService;
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
    private final CommunityStore community;
    private final ReviewAccessService reviewAccess;
    private final AiModerationStore ai;
    private final AiTriageProperties aiProperties;
    private final com.FoodtimeNeo.venue.VenueReviewService venues;
    private final com.FoodtimeNeo.community.service.CommunityNotificationService communityNotifications;
    public ModerationService(ModerationStore store, PublishedReviewMapper reviews, CatalogSubmissionMapper catalog,
                             CommunityStore community, ReviewAccessService reviewAccess,
                             AiModerationStore ai, AiTriageProperties aiProperties, com.FoodtimeNeo.venue.VenueReviewService venues,
                             com.FoodtimeNeo.community.service.CommunityNotificationService communityNotifications) {
        this.store = store; this.reviews = reviews; this.catalog = catalog; this.community = community; this.reviewAccess=reviewAccess;
        this.ai=ai;this.aiProperties=aiProperties;this.venues=venues;this.communityNotifications=communityNotifications;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<ModerationItem> queue(UserProfile actor, String status, PageRequest page) {
        return queue(actor,status,null,page);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
    public PageResponse<ModerationItem> queue(UserProfile actor, String status, String type, PageRequest page) {
        guard(actor);
        if (!Set.of("pending","approved","rejected").contains(status)) throw bad("审核状态不合法");
        if (type!=null&&!com.FoodtimeNeo.venue.VenueReviewService.isType(type)&&!Set.of("review","dish","stall","photo","dish-review-reply").contains(type)) throw bad("审核类型不合法");
        return PageResponse.of(store.page(status,type,page.size(),page.offset()),store.count(status,type),page);
    }

    @Transactional
    public ModerationResult moderate(UserProfile actor, String type, UUID id, @Valid ModerationRequest request) {
        guard(actor);
        if(com.FoodtimeNeo.venue.VenueReviewService.isType(type))return venues.moderate(actor,type,id,request);
        if (!Set.of("review","dish","stall","photo","community-post","community-comment","dish-review-reply").contains(type)) throw bad("审核类型不合法");
        ModerationItem item = store.lock(type,id);
        if (item == null) throw new BusinessException("SUBMISSION_NOT_FOUND", "提交不存在", HttpStatus.NOT_FOUND);
        if(type.equals("community-post")) {
            long current=community.contentRevision(id);
            if((request.contentRevision()==null&&current>1)||(request.contentRevision()!=null&&request.contentRevision()!=current))
                throw new BusinessException("POST_REVIEW_CONFLICT","帖子内容已变化，请重新读取后审核",HttpStatus.CONFLICT);
        }
        if (!item.status().equals("pending")) throw new BusinessException("ALREADY_REVIEWED", "此提交已审核", HttpStatus.CONFLICT);
        String reason = request.reason() == null ? null : request.reason().strip();
        if (request.action().equals("reject") && (reason == null || reason.isBlank())) throw bad("请填写驳回原因");
        BigDecimal price = item.price();
        if (request.action().equals("approve") && type.equals("dish")) {
            price = request.price() == null ? item.price() : request.price();
            if (price == null) throw bad("审核通过前请补齐菜品价格");
        }
        return decide(type, item, request.action(), reason, price, actor.id(), "human", null);
    }

    /**
     * Catalog uploads are rejection-only, irrespective of approval thresholds or model mode.
     * Races with a human decision surface as ALREADY_REVIEWED for the caller to absorb.
     */
    @Transactional
    public ModerationResult autoDecide(String type, UUID id, com.FoodtimeNeo.ai.dto.AiVerdict verdict, boolean approve) {
        boolean catalogUpload=Set.of("dish","photo").contains(type);
        if (!Set.of("review", "community-post", "community-comment", "dish-review-reply","dish","photo").contains(type)) throw bad("AI自动审核类型不合法");
        if(catalogUpload&&approve)throw bad("菜品与菜品图片必须由管理员人工通过");
        if(catalogUpload&&!catalogRejectable(verdict))throw bad("仅可对高置信度高风险菜品或图片执行AI驳回");
        ModerationItem item = store.lock(type,id);
        if(type.startsWith("community-") && !community.aiPending(type,id))
            throw new BusinessException("ALREADY_REVIEWED","内容已转人工审核或已处理",HttpStatus.CONFLICT);
        if (item == null) throw new BusinessException("SUBMISSION_NOT_FOUND", "提交不存在", HttpStatus.NOT_FOUND);
        if (!item.status().equals("pending")) throw new BusinessException("ALREADY_REVIEWED", "此提交已审核", HttpStatus.CONFLICT);
        return decide(type, item, approve ? "approve" : "reject", verdict.reason(), item.price(), null, "ai", verdict.model());
    }

    private boolean catalogRejectable(AiVerdict verdict) {
        return "HIGH".equals(verdict.riskLevel())&&Double.isFinite(verdict.confidence())
                &&verdict.confidence()>=aiProperties.autoRejectAbove()&&verdict.confidence()<=1
                &&!"system".equals(verdict.model());
    }

    /** AI network work happens outside a transaction; only its original version may be persisted. */
    @Transactional
    public int recordPostVerdict(UUID id,long revision,AiVerdict verdict) {
        if(!community.lockAiRevision(id,revision))return -1;
        return ai.insert(UUID.randomUUID(),"community-post",id,verdict.riskLevel(),BigDecimal.valueOf(verdict.confidence()),
                verdict.labels().toArray(String[]::new),verdict.reason(),verdict.model());
    }

    @Transactional
    public void recordPostImageVerdict(UUID post,long revision,UUID image,AiVerdict verdict) {
        if(!community.lockAiRevision(post,revision))return;
        if(community.postImages(post).stream().noneMatch(value->value.id().equals(image)))return;
        ai.insert(UUID.randomUUID(),"community-image",image,verdict.riskLevel(),BigDecimal.valueOf(verdict.confidence()),
                verdict.labels().toArray(String[]::new),verdict.reason(),verdict.model());
    }

    @Transactional
    public ModerationResult autoDecidePost(UUID id,long revision,AiVerdict verdict,boolean approve) {
        if(!community.lockAiRevision(id,revision))return null;
        return autoDecide("community-post",id,verdict,approve);
    }

    /** AI calls happen outside this transaction. Record and optional rejection commit together;
     * locking the submission first serializes competing workers and human decisions. */
    @Transactional
    public int recordCatalogVerdict(String type,UUID id,AiVerdict verdict) {
        if(!Set.of("dish","photo").contains(type))throw bad("非菜品上传的AI结论");
        ModerationItem item=store.lock(type,id);
        if(item==null||!item.status().equals("pending"))return 0;
        int inserted=ai.insert(UUID.randomUUID(),type,id,verdict.riskLevel(),BigDecimal.valueOf(verdict.confidence()),
                verdict.labels().toArray(String[]::new),verdict.reason(),verdict.model());
        AiVerdict canonical=inserted==1?verdict:ai.catalogVerdict(type,id).orElseThrow();
        if(catalogRejectable(canonical))
            decide(type,item,"reject",canonical.reason(),item.price(),null,"ai",canonical.model());
        return inserted;
    }

    private ModerationResult decide(String type, ModerationItem item, String action, String reason,
                                    BigDecimal price, UUID moderatorId, String actor, String aiModel) {
        UUID approved = null;
        if (action.equals("approve")) {
            boolean visible = switch (type) {
                case "review", "photo" -> reviews.lockVisibleDish(item.targetId()) != null;
                case "dish-review-reply" -> reviewAccess.visible(item.targetId());
                case "dish" -> catalog.lockStall(item.targetId()) != null;
                case "community-post", "community-comment" -> true;
                default -> catalog.lockHall(item.targetId()) != null;
            };
            if (!visible) throw new BusinessException("TARGET_UNAVAILABLE", "目标已停用，请先恢复", HttpStatus.CONFLICT);
            try {
                if (type.equals("review")) approved = store.publishReview(item);
                else if (type.equals("dish")) approved = store.publishDish(item,price,moderatorId,reason);
                else if(type.equals("stall")) approved = store.publishStall(item);
                else if(type.equals("community-post")) approved = item.id();
                else approved = item.id();
            } catch (DuplicateKeyException conflict) {
                throw new BusinessException("CATALOG_CONFLICT", "同一食堂已有同名档口", HttpStatus.CONFLICT);
            }
        }
        String status = action.equals("approve") ? "approved" : "rejected";
        store.complete(item,moderatorId,aiModel,status,reason,approved,price);
        if (type.equals("community-post") && action.equals("approve")) community.bumpRepostOf(item.id());
        store.log(item,moderatorId,actor,aiModel,action,reason);
        if (type.equals("community-comment") && action.equals("approve")) communityNotifications.commentApproved(item.id());
        return new ModerationResult(item.id(),status,approved);
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
