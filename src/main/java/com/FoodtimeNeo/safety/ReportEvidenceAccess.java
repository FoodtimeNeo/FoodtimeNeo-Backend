package com.FoodtimeNeo.safety;

import com.FoodtimeNeo.authz.AuthorizationContext;
import com.FoodtimeNeo.authz.Capability;
import com.FoodtimeNeo.authz.CapabilityService;
import com.FoodtimeNeo.media.service.StagedImageAccessPolicy;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Report/appeal evidence is limited to the uploader and report/appeal reviewers. */
@Component
public class ReportEvidenceAccess implements StagedImageAccessPolicy {
    private final ReportInteractionStore interactions;
    private final CapabilityService capabilities;

    public ReportEvidenceAccess(ReportInteractionStore interactions, CapabilityService capabilities) {
        this.interactions = interactions;
        this.capabilities = capabilities;
    }

    @Override
    public boolean restricted(UserProfile user, UUID imageId) {
        String url = "/api/v1/media/" + imageId;
        if (!interactions.evidenceImage(url)) return false;
        return !capabilities.allows(user, Capability.REPORT_REVIEW, AuthorizationContext.none())
                && !capabilities.allows(user, Capability.APPEAL_REVIEW, AuthorizationContext.none());
    }
}
