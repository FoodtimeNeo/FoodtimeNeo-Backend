package com.FoodtimeNeo.authz;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilityServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-12T00:00:00Z");

    private static UserProfile actor(String role) {
        return new UserProfile(UUID.randomUUID(), "actor@bjtu.edu.cn", "测试用户", role, "active", NOW);
    }

    private static final class MutablePolicy implements AuthorizationPolicy {
        Set<Capability> adminCapabilities = Set.of();
        String revision = "test-v1";

        @Override
        public boolean allows(UserProfile actor, Capability capability, AuthorizationContext context) {
            return capabilities(actor).contains(capability);
        }

        @Override
        public Set<Capability> capabilities(UserProfile actor) {
            return "admin".equals(actor.role()) ? adminCapabilities : Set.of();
        }

        @Override
        public String revision() {
            return revision;
        }
    }

    @Test
    void replacingOnlyThePolicyCanDelegateOneCapabilityWithoutGrantingAnother() {
        MutablePolicy policy = new MutablePolicy();
        CapabilityService service = new CapabilityService(policy, Clock.fixed(NOW, ZoneOffset.UTC));
        UserProfile admin = actor("admin");

        assertThat(service.allows(admin, Capability.REPORT_REPLY, AuthorizationContext.none())).isFalse();
        assertThat(service.allows(admin, Capability.REPORT_REVIEW, AuthorizationContext.none())).isFalse();

        policy.adminCapabilities = Set.of(Capability.REPORT_REPLY);
        policy.revision = "test-v2";
        assertThat(service.allows(admin, Capability.REPORT_REPLY, AuthorizationContext.none())).isTrue();
        assertThat(service.allows(admin, Capability.REPORT_REVIEW, AuthorizationContext.none())).isFalse();
        assertThat(service.snapshot(admin).capabilities()).containsExactly("report.reply");
    }

    @Test
    void roleLevelSnapshotUsesTheRevisionAsItsCacheKey() {
        MutablePolicy policy = new MutablePolicy();
        policy.adminCapabilities = Set.of(Capability.CATALOG_EDIT);
        CapabilityService service = new CapabilityService(policy, Clock.fixed(NOW, ZoneOffset.UTC));
        UserProfile admin = actor("admin");

        assertThat(service.capabilities(admin)).containsExactly(Capability.CATALOG_EDIT);
        policy.adminCapabilities = Set.of(Capability.CATALOG_EDIT, Capability.REPORT_REPLY);
        assertThat(service.capabilities(admin)).containsExactly(Capability.CATALOG_EDIT);
        policy.revision = "test-v2";
        assertThat(service.capabilities(admin))
                .containsExactlyInAnyOrder(Capability.CATALOG_EDIT, Capability.REPORT_REPLY);
    }

    @Test
    void requireReturnsTheStableAccessDeniedError() {
        CapabilityService service = new CapabilityService(new MutablePolicy(), Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> service.require(actor("admin"), Capability.REPORT_REVIEW, AuthorizationContext.none()))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("ACCESS_DENIED");
    }

    @Test
    void snapshotIsSortedAndDisabledAccountsAreEmpty() {
        MutablePolicy policy = new MutablePolicy();
        policy.adminCapabilities = Set.of(Capability.MODERATION_REVIEW, Capability.CATALOG_EDIT);
        CapabilityService service = new CapabilityService(policy, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(service.snapshot(actor("admin")).capabilities())
                .containsExactly("catalog.edit", "moderation.review");
        UserProfile disabled = new UserProfile(UUID.randomUUID(), "x@bjtu.edu.cn", "x", "superadmin", "disabled", NOW);
        assertThat(service.capabilities(disabled)).isEmpty();
        assertThat(service.allows(disabled, Capability.REPORT_REVIEW, AuthorizationContext.none())).isFalse();
    }

    @Test
    void invalidateDropsCachedSnapshots() {
        MutablePolicy policy = new MutablePolicy();
        policy.adminCapabilities = Set.of(Capability.CATALOG_EDIT);
        CapabilityService service = new CapabilityService(policy, Clock.fixed(NOW, ZoneOffset.UTC));
        UserProfile admin = actor("admin");
        assertThat(service.capabilities(admin)).containsExactly(Capability.CATALOG_EDIT);
        policy.adminCapabilities = Set.of(Capability.REPORT_REPLY);
        service.invalidate();
        assertThat(service.capabilities(admin)).containsExactly(Capability.REPORT_REPLY);
    }
}
