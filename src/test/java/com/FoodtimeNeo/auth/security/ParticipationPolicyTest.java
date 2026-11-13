package com.FoodtimeNeo.auth.security;

import com.FoodtimeNeo.config.properties.ParticipationProperties;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class ParticipationPolicyTest {
    private UserProfile actor(String role, String status) {
        return new UserProfile(UUID.randomUUID(), "member@gmail.com", "member", role, status, Instant.EPOCH);
    }

    @Test void openingAccessProjectsOnlyActiveVisitorsWithoutChangingTheStoredIdentity() {
        var policy = new ParticipationPolicy(new ParticipationProperties(false));
        var stored = actor("visitor", "active");
        var effective = policy.resolve(stored);
        assertThat(effective.role()).isEqualTo("user");
        assertThat(effective.id()).isEqualTo(stored.id());
        assertThat(effective.email()).isEqualTo(stored.email());
        assertThat(effective.passwordChangedAt()).isEqualTo(stored.passwordChangedAt());
        assertThat(stored.role()).isEqualTo("visitor");
        for (String role : new String[]{"user", "admin", "superadmin", "unknown"}) {
            var other = actor(role, "active");
            assertThat(policy.resolve(other)).isSameAs(other);
        }
        for (String status : new String[]{"disabled", "deleted"}) {
            var blocked = actor("visitor", status);
            assertThat(policy.resolve(blocked)).isSameAs(blocked);
        }
        assertThat(policy.resolve(null)).isNull();
    }

    @Test void requiringSchoolEmailRestoresTheOriginalRoleWithoutADataMigration() {
        var stored = actor("visitor", "active");
        assertThat(new ParticipationPolicy(new ParticipationProperties(false)).resolve(stored).role()).isEqualTo("user");
        assertThat(new ParticipationPolicy(new ParticipationProperties(true)).resolve(stored)).isSameAs(stored);
    }
}
