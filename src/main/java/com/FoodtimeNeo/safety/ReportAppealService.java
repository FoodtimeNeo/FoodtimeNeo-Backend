package com.FoodtimeNeo.safety;

import com.FoodtimeNeo.authz.AuthorizationContext;
import com.FoodtimeNeo.authz.Capability;
import com.FoodtimeNeo.authz.CapabilityService;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.notification.NotificationCategory;
import com.FoodtimeNeo.notification.NotificationPublisher;
import com.FoodtimeNeo.safety.ReportInteractionDtos.Appeal;
import com.FoodtimeNeo.safety.ReportInteractionDtos.AppealRequest;
import com.FoodtimeNeo.safety.ReportInteractionDtos.AppealResolveRequest;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ReportAppealService {
    private final ReportInteractionStore interactions;
    private final CapabilityService capabilities;
    private final ImageStorageService images;
    private final NotificationPublisher notifications;
    private final Clock clock;

    public ReportAppealService(ReportInteractionStore interactions, CapabilityService capabilities,
            ImageStorageService images, NotificationPublisher notifications, Clock clock) {
        this.interactions = interactions;
        this.capabilities = capabilities;
        this.images = images;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public Appeal submit(UserProfile actor, UUID decisionId, AppealRequest request) {
        ReportInteractionStore.Recipient recipient = interactions.recipient(decisionId, actor.id());
        if (recipient == null) throw notFound();
        String content = text(request.content(), 2000, "申诉内容");
        List<String> evidence = evidence(actor, request.evidenceUrls());
        Appeal replay = interactions.findAppealByRequest(actor.id(), request.clientRequestId());
        if (replay != null) {
            if (!replay.content().equals(content) || !replay.evidenceUrls().equals(evidence))
                throw conflict("APPEAL_REQUEST_REUSED", "请求编号已用于不同内容");
            return userView(replay);
        }
        if (!clock.instant().isBefore(recipient.appealDeadline())) {
            throw new BusinessException("APPEAL_DEADLINE_EXPIRED", "申诉期限已过", HttpStatus.CONFLICT);
        }
        if (interactions.existingAppeal(decisionId, actor.id()) != null)
            throw new BusinessException("APPEAL_ALREADY_SUBMITTED", "该决定已提交过申诉", HttpStatus.CONFLICT);
        try {
            return userView(interactions.insertAppeal(UUID.randomUUID(), decisionId, actor.id(),
                    recipient.side(), content, request.clientRequestId(), evidence));
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            throw new BusinessException("APPEAL_ALREADY_SUBMITTED", "该决定已提交过申诉", HttpStatus.CONFLICT);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<Appeal> mine(UserProfile actor, PageRequest page) {
        return PageResponse.of(interactions.appealsForUser(actor.id(), page.size(), page.offset()).stream()
                        .map(ReportAppealService::userView).toList(),
                interactions.appealsForUserCount(actor.id()), page);
    }

    @Transactional(readOnly = true)
    public Appeal mine(UserProfile actor, UUID id) {
        Appeal appeal = interactions.appealForUser(id, actor.id());
        if (appeal == null) throw notFound();
        return userView(appeal);
    }

    @Transactional(readOnly = true)
    public PageResponse<Appeal> adminQueue(UserProfile actor, String status, PageRequest page) {
        capabilities.require(actor, Capability.APPEAL_REVIEW, AuthorizationContext.none());
        String normalized = status == null || status.isBlank() ? null : status.strip();
        if (normalized != null && !Set.of("submitted", "under_review", "resolved").contains(normalized))
            throw bad("status不合法");
        return PageResponse.of(interactions.appeals(normalized, page.size(), page.offset()),
                interactions.appealsCount(normalized), page);
    }

    @Transactional(readOnly = true)
    public Appeal adminDetail(UserProfile actor, UUID id) {
        capabilities.require(actor, Capability.APPEAL_REVIEW, AuthorizationContext.none());
        Appeal appeal = interactions.appeal(id);
        if (appeal == null) throw notFound();
        return appeal;
    }

    @Transactional
    public Appeal start(UserProfile actor, UUID id, long expectedRevision) {
        capabilities.require(actor, Capability.APPEAL_REVIEW, AuthorizationContext.none());
        if (!interactions.startAppeal(id, expectedRevision)) throw conflict("APPEAL_REVISION_CONFLICT",
                "申诉已被其他操作修改，请刷新后重试");
        return adminDetail(actor, id);
    }

    @Transactional
    public Appeal resolve(UserProfile actor, UUID id, AppealResolveRequest request) {
        capabilities.require(actor, Capability.APPEAL_REVIEW, AuthorizationContext.none());
        String outcome = request.outcome().strip();
        if (!"upheld".equals(outcome) && !"rejected".equals(outcome)) throw bad("outcome不合法");
        String reason = text(request.publicReason(), 500, "公开理由");
        String note = request.internalNote() == null ? null : text(request.internalNote(), 2000, "内部备注");
        Appeal existing = interactions.appeal(id);
        if (existing == null) throw notFound();
        if (!interactions.resolveAppeal(id, request.expectedRevision(), outcome, reason, note, actor.id()))
            throw conflict("APPEAL_REVISION_CONFLICT", "申诉已被其他操作修改，请刷新后重试");
        Appeal resolved = interactions.appeal(id);
        String title = "申诉复核结果：" + ("upheld".equals(outcome) ? "成立" : "不成立");
        for (ReportInteractionStore.Recipient recipient : interactions.recipients(resolved.decisionId())) {
            notifications.publish(NotificationCategory.APPEAL, recipient.userId(), UUID.randomUUID(),
                    title, "公开理由：" + reason, "report-appeal", id, null, null);
        }
        return resolved;
    }

    private List<String> evidence(UserProfile actor, List<String> urls) {
        List<String> result = new ArrayList<>();
        if (urls == null) return result;
        if (urls.size() > 3) throw bad("证据图片最多3张");
        for (String value : urls) {
            if (value == null || value.isBlank()) throw bad("证据图片引用不能为空");
            String url = value.strip();
            if (result.contains(url)) throw bad("证据图片不能重复");
            images.requireOwnedReference(url, actor.id());
            result.add(url);
        }
        return result;
    }

    private static Appeal userView(Appeal value) {
        return new Appeal(value.id(), value.decisionId(), value.appellantId(), value.appellantName(),
                value.side(), value.content(), value.status(), value.outcome(), value.publicReason(),
                null, value.revision(), value.createdAt(), value.resolvedAt(), value.evidenceUrls());
    }

    private static String text(String value, int max, String label) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw bad(label + "不能为空");
        if (normalized.codePointCount(0, normalized.length()) > max) throw bad(label + "不能超过" + max + "个字符");
        return normalized;
    }

    private static BusinessException notFound() {
        return new BusinessException("APPEAL_NOT_FOUND", "申诉不存在", HttpStatus.NOT_FOUND);
    }

    private static BusinessException bad(String message) {
        return new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }
}
