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
import com.FoodtimeNeo.safety.ReportInteractionDtos.FollowUp;
import com.FoodtimeNeo.safety.ReportInteractionDtos.FollowUpRequest;
import com.FoodtimeNeo.safety.ReportInteractionDtos.StaffFollowUpRequest;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReportFollowUpService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int DAILY_LIMIT = 5;
    private final ReportInteractionStore interactions;
    private final CapabilityService capabilities;
    private final ImageStorageService images;
    private final NotificationPublisher notifications;
    private final Clock clock;

    public ReportFollowUpService(ReportInteractionStore interactions, CapabilityService capabilities,
            ImageStorageService images, NotificationPublisher notifications, Clock clock) {
        this.interactions = interactions;
        this.capabilities = capabilities;
        this.images = images;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public FollowUp addReporter(UserProfile actor, UUID reportId, FollowUpRequest request) {
        requireReporter(actor, reportId);
        String content = text(request.content(), 500, "追问内容");
        List<String> evidence = evidence(actor, request.evidenceUrls());
        LocalDate day = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        interactions.lockFollowUpDay(actor.id(), day);
        FollowUp replay = interactions.findFollowUpByRequest(actor.id(), request.clientRequestId());
        if (replay != null) {
            if (!same(replay, content, evidence)) throw conflict("REPORT_FOLLOWUP_REQUEST_REUSED",
                    "请求编号已用于不同内容");
            return userView(replay);
        }
        java.time.Instant start = day.atStartOfDay(BUSINESS_ZONE).toInstant();
        java.time.Instant end = day.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        if (interactions.followUpToday(actor.id(), start, end) >= DAILY_LIMIT) {
            throw new BusinessException("REPORT_FOLLOWUP_DAILY_LIMIT", "今天最多追加5次追问",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
        FollowUp created = interactions.insertFollowUp(UUID.randomUUID(), reportId, actor.id(),
                "reporter", content, null, request.clientRequestId(), evidence);
        return userView(created);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUp> thread(UserProfile actor, UUID reportId, PageRequest page) {
        requireReporter(actor, reportId);
        return PageResponse.of(interactions.followUps(reportId, false, page.size(), page.offset()).stream()
                        .map(ReportFollowUpService::userView).toList(),
                interactions.followUpCount(reportId), page);
    }

    @Transactional
    public FollowUp addStaff(UserProfile actor, UUID reportId, StaffFollowUpRequest request) {
        capabilities.require(actor, Capability.REPORT_REVIEW, AuthorizationContext.none());
        if (interactions.reporter(reportId) == null) throw reportNotFound();
        String content = text(request.content(), 500, "公开回复");
        String note = request.internalNote() == null ? null : text(request.internalNote(), 2000, "内部备注");
        List<String> evidence = evidence(actor, request.evidenceUrls());
        FollowUp replay = interactions.findFollowUpByRequest(actor.id(), request.clientRequestId());
        if (replay != null) return replay;
        interactions.insertStaffFollowUp(UUID.randomUUID(), reportId, actor.id(), content, note,
                request.clientRequestId(), evidence);
        UUID reporter = interactions.reporter(reportId);
        notifications.publish(NotificationCategory.REPORT, reporter, UUID.randomUUID(),
                "举报有新回复", "你的举报收到一条公开回复，请到举报回执查看。",
                "report", reportId, null, null);
        return interactions.findFollowUpByRequest(actor.id(), request.clientRequestId());
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUp> adminThread(UserProfile actor, UUID reportId, PageRequest page) {
        capabilities.require(actor, Capability.REPORT_REVIEW, AuthorizationContext.none());
        if (interactions.reporter(reportId) == null) throw reportNotFound();
        return PageResponse.of(interactions.followUps(reportId, true, page.size(), page.offset()),
                interactions.followUpCount(reportId), page);
    }

    private void requireReporter(UserProfile actor, UUID reportId) {
        UUID reporter = interactions.reporter(reportId);
        if (reporter == null || !reporter.equals(actor.id())) throw reportNotFound();
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

    private static boolean same(FollowUp existing, String content, List<String> evidence) {
        return existing.content().equals(content) && existing.evidenceUrls().equals(evidence);
    }

    private static FollowUp userView(FollowUp value) {
        return new FollowUp(value.id(), value.reportId(), value.authorRole(), value.authorName(),
                value.content(), null, value.evidenceUrls(), value.createdAt());
    }

    private static String text(String value, int max, String label) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw bad(label + "不能为空");
        if (normalized.codePointCount(0, normalized.length()) > max) throw bad(label + "不能超过" + max + "个字符");
        return normalized;
    }

    private static BusinessException reportNotFound() {
        return new BusinessException("REPORT_NOT_FOUND", "举报不存在", HttpStatus.NOT_FOUND);
    }

    private static BusinessException bad(String message) {
        return new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }
}
