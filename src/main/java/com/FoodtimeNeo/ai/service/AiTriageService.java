package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.mapper.AiModerationStore;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.config.properties.AiTriageProperties;
import com.FoodtimeNeo.media.service.ImageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Async triage: diagnoses pending reviews and photos, records verdicts, never approves or rejects.
 * Unchecked items stay in the human queue; transport failures leave no row and are retried next tick.
 */
@Service
@ConditionalOnProperty(prefix = "app.ai.triage", name = "enabled", havingValue = "true")
public class AiTriageService {
    private static final Logger LOG = LoggerFactory.getLogger(AiTriageService.class);
    private final AiModerationStore store;
    private final OpenAiCompatibleProvider openai;
    private final FakeAiProvider fake;
    private final ImageStorageService images;
    private final AiTriageProperties properties;

    public AiTriageService(AiModerationStore store, OpenAiCompatibleProvider openai, FakeAiProvider fake,
                           ImageStorageService images, AiTriageProperties properties) {
        this.store = store; this.openai = openai; this.fake = fake;
        this.images = images; this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.ai.triage.interval:PT5M}",
            initialDelayString = "${app.ai.triage.initial-delay:PT1M}")
    public void scheduled() {
        triageOnce();
    }

    /** Runs one batch; returns the number of submissions diagnosed. */
    public int triageOnce() {
        AiProvider provider = "fake".equals(properties.provider()) ? fake : openai;
        int checked = 0;
        for (var item : store.pendingReviews(properties.batchSize())) {
            try {
                AiVerdict verdict = provider.checkReview(item.rating(), item.content());
                checked += store.insert(UUID.randomUUID(), "review", item.id(), verdict.riskLevel(),
                        BigDecimal.valueOf(verdict.confidence()), verdict.labels().toArray(new String[0]),
                        verdict.reason(), verdict.model());
            } catch (AiUnavailableException exception) {
                LOG.warn("AI review triage deferred for {}: {}", item.id(), exception.getCode());
            } catch (RuntimeException exception) {
                LOG.error("AI review triage failed for {}", item.id(), exception);
                break;
            }
        }
        for (var item : store.pendingPhotos(properties.batchSize())) {
            try {
                // Staged files are addressed by the file id embedded in the image reference,
                // which differs from the dish_images row id.
                UUID fileId = fileId(item.imageUrl());
                ImageStorageService.StagedFile thumbnail = images.stagedThumbnail(fileId);
                AiVerdict verdict = provider.checkImage(thumbnail.bytes(), thumbnail.contentType());
                checked += store.insert(UUID.randomUUID(), "photo", item.id(), verdict.riskLevel(),
                        BigDecimal.valueOf(verdict.confidence()), verdict.labels().toArray(new String[0]),
                        verdict.reason(), verdict.model());
            } catch (AiUnavailableException exception) {
                LOG.warn("AI photo triage deferred for {}: {}", item.id(), exception.getCode());
            } catch (BusinessException missing) {
                if (!"IMAGE_NOT_FOUND".equals(missing.getCode())) throw missing;
                // Staging file lost (manual deletion or volume loss): flag it so a human rejects the submission.
                checked += store.insert(UUID.randomUUID(), "photo", item.id(), "HIGH", BigDecimal.ONE,
                        new String[] {"文件缺失"}, missing.getMessage() + "，无法完成审核", "system");
            } catch (IOException exception) {
                LOG.warn("AI photo triage deferred for {}: staging file unreadable", item.id());
            } catch (RuntimeException exception) {
                LOG.error("AI photo triage failed for {}", item.id(), exception);
                break;
            }
        }
        if (checked > 0) LOG.info("AI triage diagnosed {} submissions", checked);
        return checked;
    }

    /** Staged files are addressed by the file id embedded in the image reference. */
    private UUID fileId(String imageUrl) {
        try {
            if (imageUrl == null || !imageUrl.startsWith("/api/v1/media/")) throw new IllegalArgumentException();
            return UUID.fromString(imageUrl.substring("/api/v1/media/".length()));
        } catch (IllegalArgumentException invalid) {
            throw new BusinessException("IMAGE_NOT_FOUND", "图片引用无效", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
