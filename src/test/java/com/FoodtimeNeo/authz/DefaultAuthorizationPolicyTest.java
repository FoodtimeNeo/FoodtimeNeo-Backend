package com.FoodtimeNeo.authz;

import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAuthorizationPolicyTest {
    private static UserProfile actor(String role, String status) {
        return new UserProfile(UUID.randomUUID(), "actor@bjtu.edu.cn", "测试用户", role, status, Instant.now());
    }

    @Test
    void superadminReceivesEveryCurrentCapability() {
        UserProfile actor = actor("superadmin", "active");
        var policy = new DefaultAuthorizationPolicy();
        assertThat(policy.capabilities(actor)).containsExactlyInAnyOrder(Capability.values());
        assertThat(policy.allows(actor, Capability.REPORT_REVIEW, AuthorizationContext.none())).isTrue();
    }

    @Test
    void normalAdminKeepsOnlyCatalogAndModeration() {
        UserProfile actor = actor("admin", "active");
        var policy = new DefaultAuthorizationPolicy();
        assertThat(policy.capabilities(actor))
                .containsExactlyInAnyOrder(Capability.CATALOG_EDIT, Capability.MODERATION_REVIEW);
        assertThat(policy.allows(actor, Capability.CORRECTION_REVIEW, AuthorizationContext.none())).isFalse();
        assertThat(policy.allows(actor, Capability.REPORT_REPLY, AuthorizationContext.none())).isFalse();
        assertThat(policy.allows(actor, Capability.APPEAL_REVIEW, AuthorizationContext.none())).isFalse();
    }

    @Test
    void ordinaryUsersAndVisitorsHaveNoManagementCapability() {
        var policy = new DefaultAuthorizationPolicy();
        assertThat(policy.capabilities(actor("user", "active"))).isEmpty();
        assertThat(policy.capabilities(actor("visitor", "active"))).isEmpty();
    }

    @Test
    void disabledAccountsLoseAllCapabilities() {
        var policy = new DefaultAuthorizationPolicy();
        assertThat(policy.capabilities(actor("superadmin", "disabled"))).isEmpty();
        assertThat(policy.allows(actor("superadmin", "disabled"),
                Capability.REPORT_REVIEW, AuthorizationContext.none())).isFalse();
        assertThat(policy.capabilities(null)).isEmpty();
    }

    @Test
    void revisionIsStableForTheDefaultMapping() {
        var policy = new DefaultAuthorizationPolicy();
        assertThat(policy.revision()).isEqualTo("default-v1");
    }

    @Test
    void capabilityNamesRoundTripOnTheWire() {
        assertThat(Capability.REPORT_REVIEW.wireName()).isEqualTo("report.review");
        assertThat(Capability.fromWire(" REPORT.REVIEW ")).contains(Capability.REPORT_REVIEW);
        assertThat(Capability.fromWire("unknown.action")).isEmpty();
        assertThat(Set.of(Capability.values())).hasSize(Capability.values().length);
    }
}
