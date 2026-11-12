package com.FoodtimeNeo.safety;

import com.FoodtimeNeo.authz.AuthorizationContext;
import com.FoodtimeNeo.authz.Capability;
import com.FoodtimeNeo.authz.CapabilityService;
import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.api.PageResponse;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.notification.NotificationCategory;
import com.FoodtimeNeo.notification.NotificationPublisher;
import com.FoodtimeNeo.safety.ReportDecisionDtos.DecisionRequest;
import com.FoodtimeNeo.safety.ReportDecisionDtos.DecisionResponse;
import com.FoodtimeNeo.safety.ReportDecisionDtos.MyDecision;
import com.FoodtimeNeo.safety.ReportDecisionDtos.MyReport;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class ReportDecisionService {
    private final ReportDecisionStore decisions;
    private final CapabilityService capabilities;
    private final NotificationPublisher notifications;
    private final Clock clock;

    public ReportDecisionService(ReportDecisionStore decisions, CapabilityService capabilities,
            NotificationPublisher notifications, Clock clock) {
        this.decisions = decisions;
        this.capabilities = capabilities;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public DecisionResponse decide(UserProfile actor, UUID reportId, DecisionRequest request) {
        capabilities.require(actor, Capability.REPORT_REVIEW, AuthorizationContext.none());
        String outcome = request.outcome().strip();
        if (!"upheld".equals(outcome) && !"not_upheld".equals(outcome)) throw bad("outcome不合法");
        String reason = text(request.publicReason(), 500, "公开理由");
        String note = request.internalNote() == null ? null : text(request.internalNote(), 2000, "内部备注");

        ReportDecisionStore.ReportRow report = decisions.lockReport(reportId);
        if (report == null) throw new BusinessException("REPORT_NOT_FOUND", "举报不存在", HttpStatus.NOT_FOUND);
        if (report.reviewedAt() != null) throw new BusinessException("REPORT_ALREADY_DECIDED",
                "该举报已处理", HttpStatus.CONFLICT);

        UUID decisionId = UUID.randomUUID();
        decisions.insertDecision(decisionId, reportId, outcome, reason, note, actor.id());
        java.time.Instant deliveredAt = clock.instant();
        java.time.Instant appealDeadline = deliveredAt.plus(7, ChronoUnit.DAYS);

        UUID reporterEvent = UUID.randomUUID();
        notifications.publish(NotificationCategory.REPORT, report.reporterId(), reporterEvent,
                "举报处理结果：" + outcomeLabel(outcome),
                "公开理由：" + reason, "report-decision", decisionId, null, null);
        decisions.insertRecipient(decisionId, report.reporterId(), "reporter", deliveredAt,
                appealDeadline, reporterEvent);

        UUID targetEvent = UUID.randomUUID();
        notifications.publish(NotificationCategory.REPORT, report.targetUserId(), targetEvent,
                "你收到一条内容处理决定",
                "处理结果：" + outcomeLabel(outcome) + "。公开理由：" + reason,
                "report-decision", decisionId, null, null);
        decisions.insertRecipient(decisionId, report.targetUserId(), "target", deliveredAt,
                appealDeadline, targetEvent);

        return new DecisionResponse(decisionId, reportId, outcome, reason, note, actor.id(), deliveredAt);
    }

    @Transactional(readOnly = true)
    public PageResponse<MyReport> myReports(UserProfile actor, PageRequest page) {
        return PageResponse.of(decisions.myReports(actor.id(), page.size(), page.offset()),
                decisions.myReportCount(actor.id()), page);
    }

    @Transactional(readOnly = true)
    public MyReport myReport(UserProfile actor, UUID id) {
        MyReport report = decisions.myReport(actor.id(), id);
        if (report == null) throw new BusinessException("REPORT_NOT_FOUND", "举报不存在", HttpStatus.NOT_FOUND);
        return report;
    }

    @Transactional(readOnly = true)
    public PageResponse<MyDecision> myDecisions(UserProfile actor, PageRequest page) {
        return PageResponse.of(decisions.myDecisions(actor.id(), page.size(), page.offset()).stream()
                        .map(Projection::decision).toList(),
                decisions.myDecisionCount(actor.id()), page);
    }

    @Transactional(readOnly = true)
    public MyDecision myDecision(UserProfile actor, UUID id) {
        MyDecision decision = decisions.myDecision(actor.id(), id);
        if (decision == null) throw new BusinessException("DECISION_NOT_FOUND", "决定不存在", HttpStatus.NOT_FOUND);
        return Projection.decision(decision);
    }

    private static String outcomeLabel(String outcome) {
        return "upheld".equals(outcome) ? "成立" : "不成立";
    }

    private static String text(String value, int max, String label) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) throw bad(label + "不能为空");
        if (normalized.codePointCount(0, normalized.length()) > max) throw bad(label + "不能超过" + max + "个字符");
        return normalized;
    }

    private static BusinessException bad(String message) {
        return new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    /** The target side must not receive the reportId or reporter-facing fields. */
    private static final class Projection {
        private static MyDecision decision(MyDecision value) {
            if (!"target".equals(value.side())) return value;
            return new MyDecision(value.decisionId(), null, value.side(), value.targetType(),
                    value.outcome(), value.publicReason(), value.deliveredAt(), value.appealDeadline());
        }
    }
}
