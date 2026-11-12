package com.FoodtimeNeo.safety;

import com.FoodtimeNeo.admin.dto.UserAccessRequest;
import com.FoodtimeNeo.admin.service.UserAdminService;
import com.FoodtimeNeo.authz.AuthorizationContext;
import com.FoodtimeNeo.authz.Capability;
import com.FoodtimeNeo.authz.CapabilityService;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.community.dto.RemoveCommunityRequest;
import com.FoodtimeNeo.community.service.CommunityModerationService;
import com.FoodtimeNeo.safety.ReportActionDtos.Request;
import com.FoodtimeNeo.safety.ReportActionDtos.Response;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ReportActionService {
    private static final Set<String> CONTENT_ACTIONS = Set.of("remove_post", "remove_comment");
    private static final Set<String> USER_ACTIONS = Set.of("disable_user", "restore_user");
    private final ReportActionStore actions;
    private final ReportInteractionStore recipients;
    private final CommunityModerationService community;
    private final UserAdminService users;
    private final UserMapper profiles;
    private final CapabilityService capabilities;
    private final Clock clock;

    public ReportActionService(ReportActionStore actions, ReportInteractionStore recipients,
            CommunityModerationService community, UserAdminService users, UserMapper profiles,
            CapabilityService capabilities, Clock clock) {
        this.actions = actions;
        this.recipients = recipients;
        this.community = community;
        this.users = users;
        this.profiles = profiles;
        this.capabilities = capabilities;
        this.clock = clock;
    }

    @Transactional
    public Response perform(UserProfile actor, UUID reportId, Request request) {
        capabilities.require(actor, Capability.REPORT_REVIEW, AuthorizationContext.none());
        String actionType = request.actionType().strip();
        if (!CONTENT_ACTIONS.contains(actionType) && !USER_ACTIONS.contains(actionType))
            throw bad("actionType不合法");
        if (CONTENT_ACTIONS.contains(actionType))
            capabilities.require(actor, Capability.MODERATION_REVIEW, AuthorizationContext.none());
        else capabilities.require(actor, Capability.USER_MANAGE, AuthorizationContext.none());

        ReportActionStore.CaseRow row = actions.caseForReport(reportId);
        if (row == null) throw new BusinessException("REPORT_NOT_FOUND", "举报或决定不存在", HttpStatus.NOT_FOUND);
        UUID targetId = target(row, actionType, request.targetId());
        String targetType = actionType.equals("remove_post") ? "post"
                : actionType.equals("remove_comment") ? "comment" : "user";
        Response existing = actions.existing(row.decisionId(), actionType, targetId);
        if (existing != null) return existing;

        String note = request.note() == null ? null : request.note().strip();
        if ("remove_post".equals(actionType)) {
            community.remove(actor, "posts", targetId, new RemoveCommunityRequest(note));
        } else if ("remove_comment".equals(actionType)) {
            community.remove(actor, "comments", targetId, new RemoveCommunityRequest(note));
        } else {
            UserProfile target = profiles.findProfile(targetId);
            if (target == null) throw new BusinessException("USER_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND);
            users.update(actor, targetId, new UserAccessRequest(target.role(),
                    "disable_user".equals(actionType) ? "disabled" : "active"));
        }
        UUID id = UUID.randomUUID();
        actions.insert(id, row.decisionId(), actionType, targetType, targetId, actor.id(), note);
        return new Response(id, row.decisionId(), actionType, targetType, targetId, actor.id(),
                note, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<Response> listForAdmin(UserProfile actor, UUID reportId) {
        capabilities.require(actor, Capability.REPORT_REVIEW, AuthorizationContext.none());
        ReportActionStore.CaseRow row = actions.caseForReport(reportId);
        if (row == null) throw new BusinessException("REPORT_NOT_FOUND", "举报或决定不存在", HttpStatus.NOT_FOUND);
        return actions.list(row.decisionId());
    }

    @Transactional(readOnly = true)
    public List<Response> listForRecipient(UserProfile actor, UUID decisionId) {
        if (recipients.recipient(decisionId, actor.id()) == null)
            throw new BusinessException("DECISION_NOT_FOUND", "决定不存在", HttpStatus.NOT_FOUND);
        return actions.list(decisionId);
    }

    private static UUID target(ReportActionStore.CaseRow row, String actionType, UUID requested) {
        UUID expected;
        if ("remove_post".equals(actionType)) {
            if (!"post".equals(row.targetType())) throw bad("该举报目标不是帖子");
            expected = row.targetId();
        } else if ("remove_comment".equals(actionType)) {
            if (!"comment".equals(row.targetType())) throw bad("该举报目标不是评论");
            expected = row.targetId();
        } else {
            expected = row.targetUserId();
        }
        if (requested != null && !requested.equals(expected)) throw bad("动作目标与举报决定不匹配");
        return expected;
    }

    private static BusinessException bad(String message) {
        return new BusinessException("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }
}
