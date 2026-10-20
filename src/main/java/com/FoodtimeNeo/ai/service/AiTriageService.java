package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.mapper.AiModerationStore;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.mapper.CommunityStore;
import com.FoodtimeNeo.config.properties.AiTriageProperties;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.moderation.service.ModerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Async triage with a review chain: the fast primary model screens everything,
 * LOW verdicts below the confidence threshold are escalated to a stronger model,
 * and a fallback model covers primary outages. Photos use a vision model directly.
 * Nothing is ever approved or rejected here; humans decide.
 */
@Service
@ConditionalOnProperty(prefix = "app.ai.triage", name = "enabled", havingValue = "true")
public class AiTriageService {
    private static final Logger LOG = LoggerFactory.getLogger(AiTriageService.class);
    private final AiModerationStore store;
    private final OpenAiCompatibleProvider openai;
    private final FakeAiProvider fake;
    private final ImageStorageService images;
    private final ModerationService moderation;
    private final CommunityStore community;
    private final java.util.concurrent.Executor triageExecutor;
    private final AiTriageProperties properties;

    public AiTriageService(AiModerationStore store, OpenAiCompatibleProvider openai, FakeAiProvider fake,
                           ImageStorageService images, ModerationService moderation, CommunityStore community,
                           @org.springframework.beans.factory.annotation.Qualifier("aiTriageExecutor")
                           java.util.concurrent.Executor triageExecutor,
                           AiTriageProperties properties) {
        this.store = store; this.openai = openai; this.fake = fake;
        this.images = images; this.moderation = moderation; this.community = community;
        this.triageExecutor = triageExecutor; this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.ai.triage.interval:PT5M}",
            initialDelayString = "${app.ai.triage.initial-delay:PT1M}")
    public void scheduled() {
        triageOnce();
    }

    /** Diagnoses a batch of pending submissions; returns the number of verdicts recorded. */
    public int triageOnce() {
        AiProvider provider = pick();
        int checked = 0;
        for (var item : store.pendingReviews(properties.batchSize())) {
            try {
                checked += diagnosePendingReview(provider, item);
            } catch (RuntimeException exception) {
                LOG.error("AI review triage failed for {}", item.id(), exception);
                break;
            }
        }
        for (var item : store.pendingPhotos(properties.batchSize())) {
            try {
                checked += diagnosePendingPhoto(provider, item);
            } catch (RuntimeException exception) {
                LOG.error("AI photo triage failed for {}", item.id(), exception);
                break;
            }
        }
        for (var item : store.pendingCommunityPosts(properties.batchSize())) {
            try {
                checked += diagnosePendingCommunityPost(provider, item);
            } catch (RuntimeException exception) {
                LOG.error("AI community triage failed for {}", item.id(), exception);
                break;
            }
        }
        for (var item : store.pendingCommunityComments(properties.batchSize())) {
            try {
                checked += diagnosePendingCommunityComment(provider, item);
            } catch (RuntimeException exception) {
                LOG.error("AI community comment triage failed for {}", item.id(), exception);
                break;
            }
        }
        for (var item : store.pendingReviewReplies(properties.batchSize())) {
            try { checked += diagnosePendingReviewReply(provider,item); }
            catch (RuntimeException e) { LOG.error("AI reply triage deferred for {}",item.id(),e); break; }
        }
        if (checked > 0) LOG.info("AI triage diagnosed {} submissions", checked);
        return checked;
    }

    /**
     * Instant path: a new submission is diagnosed within seconds of creation.
     * Runs after the request transaction commits so the row is visible; the work
     * itself is handed to the bounded pool (a full pool drops the task and the
     * scheduled sweep retries it). Anything failing here stays quiet.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionCreated(SubmissionDiagnoseEvent event) {
        LOG.info("AI instant received {} {}", event.type(), event.submissionId());
        try {
            triageExecutor.execute(() -> diagnoseAsync(event));
        } catch (java.util.concurrent.RejectedExecutionException full) {
            LOG.warn("AI instant pool full, deferred {} {}, sweep will retry", event.type(), event.submissionId());
        }
    }

    private void diagnoseAsync(SubmissionDiagnoseEvent event) {
        try {
            AiProvider provider = pick();
            if ("review".equals(event.type())) {
                store.pendingReview(event.submissionId()).ifPresent(item -> diagnosePendingReview(provider, item));
            } else if ("photo".equals(event.type())) {
                store.pendingPhoto(event.submissionId()).ifPresent(item -> diagnosePendingPhoto(provider, item));
            } else if ("community-post".equals(event.type())) {
                store.pendingCommunityPost(event.submissionId()).ifPresent(item -> diagnosePendingCommunityPost(provider, item));
            } else if ("dish-review-reply".equals(event.type())) {
                store.pendingReviewReply(event.submissionId()).ifPresent(item -> diagnosePendingReviewReply(provider,item));
            } else if ("community-comment".equals(event.type())) {
                store.pendingCommunityComment(event.submissionId()).ifPresent(item -> diagnosePendingCommunityComment(provider, item));
            }
        } catch (RuntimeException exception) {
            LOG.warn("AI instant triage deferred for {} {}, sweep will retry: {}",
                    event.type(), event.submissionId(), exception.toString());
        }
    }

    private AiProvider pick() {
        return "fake".equals(properties.provider()) ? fake : openai;
    }

    /** Returns 1 when a verdict was recorded, 0 when deferred to the sweep. */
    private int diagnosePendingReview(AiProvider provider, AiModerationStore.PendingReview item) {
        try {
            AiVerdict verdict = diagnoseReview(provider, item.rating(), item.content());
            int rows = store.insert(UUID.randomUUID(), "review", item.id(), verdict.riskLevel(),
                    BigDecimal.valueOf(verdict.confidence()), verdict.labels().toArray(new String[0]),
                    verdict.reason(), verdict.model());
            maybeAutoDecide(verdict, "review", item.id());
            return rows;
        } catch (AiUnavailableException exception) {
            LOG.warn("AI review triage deferred for {}: {}", item.id(), exception.getCode());
            return 0;
        }
    }

    /**
     * Hard verdicts execute immediately: confident LOW approves, confident HIGH rejects.
     * Photos and uncertain calls stay human. Races with a human decision are absorbed.
     */
    private void maybeAutoDecide(AiVerdict verdict, String type, UUID submissionId) {
        if (!"review".equals(type) && !"community-post".equals(type) && !"community-comment".equals(type) && !"dish-review-reply".equals(type)) return;
        boolean approve = verdict.riskLevel().equals("LOW") && verdict.confidence() >= properties.autoApproveAbove();
        boolean reject = verdict.riskLevel().equals("HIGH") && verdict.confidence() >= properties.autoRejectAbove();
        if (!approve && !reject) return;
        try {
            moderation.autoDecide(type, submissionId, verdict, approve);
            LOG.info("AI auto-{} review {}", approve ? "approved" : "rejected", submissionId);
        } catch (BusinessException busy) {
            LOG.warn("AI auto-decision skipped for {}: {}", submissionId, busy.getCode());
        }
    }

    /** Returns 1 when a verdict was recorded, 0 when deferred to the sweep. */
    private int diagnosePendingPhoto(AiProvider provider, AiModerationStore.PendingPhoto item) {
        try {
            ImageStorageService.StagedFile file = images.stagedThumbnail(fileId(item.imageUrl()));
            AiVerdict verdict = diagnosePhoto(provider, file.bytes(), file.contentType());
            return store.insert(UUID.randomUUID(), "photo", item.id(), verdict.riskLevel(),
                    BigDecimal.valueOf(verdict.confidence()), verdict.labels().toArray(new String[0]),
                    verdict.reason(), verdict.model());
        } catch (AiUnavailableException exception) {
            LOG.warn("AI photo triage deferred for {}: {}", item.id(), exception.getCode());
            return 0;
        } catch (BusinessException missing) {
            if (!"IMAGE_NOT_FOUND".equals(missing.getCode())) throw missing;
            // Staging file lost (manual deletion or volume loss): flag it so a human rejects the submission.
            return store.insert(UUID.randomUUID(), "photo", item.id(), "HIGH", BigDecimal.ONE,
                    new String[] {"文件缺失"}, missing.getMessage() + "，无法完成审核", "system");
        } catch (IOException exception) {
            LOG.warn("AI photo triage deferred for {}: staging file unreadable", item.id());
            return 0;
        }
    }

    /**
     * Community post: text goes through the review chain, every attached image
     * through the photo chain. Any HIGH rejects; approval needs a confident LOW text
     * and confident LOW on every image. Missing staging files are flagged, not dropped.
     */
    private int diagnosePendingCommunityPost(AiProvider provider, AiModerationStore.PendingCommunityPost item) {
        try {
            AiVerdict text = diagnoseCommunityText(provider, item.content());
            int rows = store.insert(UUID.randomUUID(), "community-post", item.id(), text.riskLevel(),
                    BigDecimal.valueOf(text.confidence()), text.labels().toArray(new String[0]),
                    text.reason(), text.model());
            boolean imagesClean = true;
            for (var image : community.postImages(item.id())) {
                AiVerdict seen = diagnoseCommunityImage(provider, item.id(), image);
                if (seen != null && seen.riskLevel().equals("HIGH")
                        && seen.confidence() >= properties.autoRejectAbove()) {
                    maybeAutoDecide(new AiVerdict("HIGH", seen.confidence(), seen.labels(),
                            "配图" + seen.reason(), seen.model()), "community-post", item.id());
                    return rows;
                }
                if (seen == null || !seen.riskLevel().equals("LOW")
                        || seen.confidence() < properties.autoApproveAbove()) {
                    imagesClean = false;
                }
            }
            if (imagesClean) maybeAutoDecide(text, "community-post", item.id());
            else if (text.riskLevel().equals("LOW") && text.confidence() >= properties.autoApproveAbove()) {
                LOG.info("AI community post {} stays human: an image needs a human look", item.id());
            }
            return rows;
        } catch (AiUnavailableException exception) {
            LOG.warn("AI community triage deferred for {}: {}", item.id(), exception.getCode());
            return 0;
        }
    }

    /** Community comment: text chain only, then the same auto thresholds. */
    private int diagnosePendingCommunityComment(AiProvider provider, AiModerationStore.PendingCommunityComment item) {
        try {
            AiVerdict verdict = diagnoseCommunityText(provider, item.content());
            int rows = store.insert(UUID.randomUUID(), "community-comment", item.id(), verdict.riskLevel(),
                    BigDecimal.valueOf(verdict.confidence()), verdict.labels().toArray(new String[0]),
                    verdict.reason(), verdict.model());
            maybeAutoDecide(verdict, "community-comment", item.id());
            return rows;
        } catch (AiUnavailableException exception) {
            LOG.warn("AI community comment triage deferred for {}: {}", item.id(), exception.getCode());
            return 0;
        }
    }

    private int diagnosePendingReviewReply(AiProvider provider,AiModerationStore.PendingReviewReply item) {
        try {
            AiVerdict verdict=diagnoseCommunityText(provider,item.content());
            int rows=store.insert(UUID.randomUUID(),"dish-review-reply",item.id(),verdict.riskLevel(),
                    BigDecimal.valueOf(verdict.confidence()),verdict.labels().toArray(new String[0]),verdict.reason(),verdict.model());
            maybeAutoDecide(verdict,"dish-review-reply",item.id());
            return rows;
        } catch(AiUnavailableException e) {LOG.warn("AI reply triage deferred for {}",item.id());return 0;}
    }

    private AiVerdict diagnoseCommunityText(AiProvider provider, String content) throws AiUnavailableException {
        AiVerdict first;
        try {
            first = provider.checkText(properties.primaryModel(), content);
        } catch (AiUnavailableException primaryFailed) {
            return diagnoseCommunityTextFallback(provider, primaryFailed, content);
        }
        if (needsEscalation(first)) {
            try {
                return provider.checkText(properties.escalationModel(), content);
            } catch (AiUnavailableException escalated) {
                LOG.warn("AI escalation deferred, keeping primary verdict");
                return first;
            }
        }
        return first;
    }

    private AiVerdict diagnoseCommunityTextFallback(AiProvider provider, AiUnavailableException primaryFailed,
                                                    String content) throws AiUnavailableException {
        if (!notBlank(properties.fallbackModel()) || properties.fallbackModel().equals(properties.primaryModel())) {
            throw primaryFailed;
        }
        LOG.warn("AI primary unavailable, trying fallback");
        return provider.checkText(properties.fallbackModel(), content);
    }

    /**
     * Diagnoses one attached image; null means deferred (sweep retries),
     * a HIGH system verdict means the staging file is gone.
     */
    private AiVerdict diagnoseCommunityImage(AiProvider provider, UUID postId, CommunityStore.ImageRef image) throws AiUnavailableException {
        try {
            ImageStorageService.StagedFile file = images.stagedThumbnail(fileId(image.url()));
            AiVerdict verdict = diagnosePhoto(provider, file.bytes(), file.contentType());
            store.insert(UUID.randomUUID(), "community-image", image.id(), verdict.riskLevel(),
                    BigDecimal.valueOf(verdict.confidence()), verdict.labels().toArray(new String[0]),
                    verdict.reason(), verdict.model());
            return verdict;
        } catch (AiUnavailableException exception) {
            LOG.warn("AI community image triage deferred for {}: {}", image.id(), exception.getCode());
            return null;
        } catch (BusinessException missing) {
            if (!"IMAGE_NOT_FOUND".equals(missing.getCode())) throw missing;
            AiVerdict gone = new AiVerdict("HIGH", 1.0, List.of("文件缺失"),
                    missing.getMessage() + "，无法完成审核", "system");
            store.insert(UUID.randomUUID(), "community-image", image.id(), "HIGH", BigDecimal.ONE,
                    new String[] {"文件缺失"}, gone.reason(), "system");
            return gone;
        } catch (IOException exception) {
            LOG.warn("AI community image triage deferred for {}: staging file unreadable", image.id());
            return null;
        }
    }

    private AiVerdict diagnoseReview(AiProvider provider, int rating, String content) throws AiUnavailableException {
        AiVerdict first;
        try {
            first = provider.checkReview(properties.primaryModel(), rating, content);
        } catch (AiUnavailableException primaryFailed) {
            return diagnoseReviewFallback(provider, primaryFailed, rating, content);
        }
        if (needsEscalation(first)) {
            try {
                return provider.checkReview(properties.escalationModel(), rating, content);
            } catch (AiUnavailableException escalated) {
                LOG.warn("AI escalation deferred, keeping primary verdict");
                return first;
            }
        }
        return first;
    }

    /**
     * Escalate when the fast model is unsure: LOW below the confidence threshold,
     * or any MEDIUM (the fast model punts edge cases there; the stronger model
     * separates them). Never escalate when no distinct escalation model is set.
     */
    private boolean needsEscalation(AiVerdict first) {
        if (!notBlank(properties.escalationModel()) || properties.escalationModel().equals(properties.primaryModel())) {
            return false;
        }
        if (first.riskLevel().equals("MEDIUM")) return true;
        return first.riskLevel().equals("LOW") && first.confidence() < properties.escalationBelowConfidence();
    }

    private AiVerdict diagnoseReviewFallback(AiProvider provider, AiUnavailableException primaryFailed,
                                             int rating, String content) throws AiUnavailableException {
        if (!notBlank(properties.fallbackModel()) || properties.fallbackModel().equals(properties.primaryModel())) {
            throw primaryFailed;
        }
        LOG.warn("AI primary unavailable, trying fallback");
        return provider.checkReview(properties.fallbackModel(), rating, content);
    }

    private AiVerdict diagnosePhoto(AiProvider provider, byte[] image, String contentType) throws AiUnavailableException {
        try {
            return provider.checkImage(properties.photoModel(), image, contentType);
        } catch (AiUnavailableException primaryFailed) {
            if (!notBlank(properties.fallbackModel()) || properties.fallbackModel().equals(properties.photoModel())) {
                throw primaryFailed;
            }
            LOG.warn("AI photo model unavailable, trying fallback");
            return provider.checkImage(properties.fallbackModel(), image, contentType);
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** Staged files are addressed by the file id embedded in the image reference. */
    private static UUID fileId(String imageUrl) {
        try {
            if (imageUrl == null || !imageUrl.startsWith("/api/v1/media/")) throw new IllegalArgumentException();
            return UUID.fromString(imageUrl.substring("/api/v1/media/".length()));
        } catch (IllegalArgumentException invalid) {
            throw new BusinessException("IMAGE_NOT_FOUND", "图片引用无效", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
