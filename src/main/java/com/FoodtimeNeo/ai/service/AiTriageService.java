package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.mapper.AiModerationStore;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.AiTriageProperties;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.moderation.service.ModerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.io.IOException;
import java.math.BigDecimal;
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
    private final AiTriageProperties properties;

    public AiTriageService(AiModerationStore store, OpenAiCompatibleProvider openai, FakeAiProvider fake,
                           ImageStorageService images, ModerationService moderation, AiTriageProperties properties) {
        this.store = store; this.openai = openai; this.fake = fake;
        this.images = images; this.moderation = moderation; this.properties = properties;
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
        if (checked > 0) LOG.info("AI triage diagnosed {} submissions", checked);
        return checked;
    }

    /**
     * Instant path: a new submission is diagnosed within seconds of creation.
     * Runs after the request transaction commits so the row is visible;
     * anything failing here stays quiet and the scheduled sweep retries it.
     */
    @Async("aiTriageExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionCreated(SubmissionDiagnoseEvent event) {
        try {
            AiProvider provider = pick();
            if ("review".equals(event.type())) {
                store.pendingReview(event.submissionId()).ifPresent(item -> diagnosePendingReview(provider, item));
            } else if ("photo".equals(event.type())) {
                store.pendingPhoto(event.submissionId()).ifPresent(item -> diagnosePendingPhoto(provider, item));
            }
        } catch (RuntimeException exception) {
            LOG.warn("AI instant triage deferred for {} {}, sweep will retry", event.type(), event.submissionId());
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
        if (!"review".equals(type)) return;
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

    private AiVerdict diagnoseReview(AiProvider provider, int rating, String content) throws AiUnavailableException {
        AiVerdict first;
        try {
            first = provider.checkReview(properties.primaryModel(), rating, content);
        } catch (AiUnavailableException primaryFailed) {
            return diagnoseReviewFallback(provider, primaryFailed, rating, content);
        }
        if (first.riskLevel().equals("LOW") && first.confidence() < properties.escalationBelowConfidence()
                && notBlank(properties.escalationModel()) && !properties.escalationModel().equals(properties.primaryModel())) {
            try {
                return provider.checkReview(properties.escalationModel(), rating, content);
            } catch (AiUnavailableException escalated) {
                LOG.warn("AI escalation deferred, keeping primary verdict");
                return first;
            }
        }
        return first;
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
