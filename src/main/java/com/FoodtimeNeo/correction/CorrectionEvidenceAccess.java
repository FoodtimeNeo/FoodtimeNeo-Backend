package com.FoodtimeNeo.correction;

import com.FoodtimeNeo.authz.AuthorizationContext;
import com.FoodtimeNeo.authz.Capability;
import com.FoodtimeNeo.authz.CapabilityService;
import com.FoodtimeNeo.media.service.StagedImageAccessPolicy;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Correction evidence stays private to its uploader and holders of correction.review. */
@Component
public class CorrectionEvidenceAccess implements StagedImageAccessPolicy {
    private final CatalogCorrectionStore corrections;
    private final CapabilityService capabilities;

    public CorrectionEvidenceAccess(CatalogCorrectionStore corrections, CapabilityService capabilities) {
        this.corrections = corrections;
        this.capabilities = capabilities;
    }

    @Override
    public boolean restricted(UserProfile user, UUID imageId) {
        UUID correctionId = corrections.evidenceCorrection("/api/v1/media/" + imageId);
        if (correctionId == null) return false;
        return !capabilities.allows(user, Capability.CORRECTION_REVIEW,
                AuthorizationContext.resource("catalog-correction", correctionId));
    }
}
