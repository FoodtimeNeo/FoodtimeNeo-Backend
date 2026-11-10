package com.FoodtimeNeo.user.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.user.entity.UserProfile;
import com.FoodtimeNeo.user.mapper.UserSearchStore;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserSearchServiceTest {
    private final UserSearchStore store = mock(UserSearchStore.class);
    private final UserSearchService service = new UserSearchService(store);
    private final UserProfile viewer = new UserProfile(UUID.randomUUID(), "private@example.test", "我", "user", "active", Instant.now());

    @Test void blankSearchNeverEnumeratesUsers() {
        assertThat(service.search(viewer, "  \t ", new PageRequest(1, 20)).items()).isEmpty();
        verifyNoInteractions(store);
    }

    @Test void oversizedQueryIsRejectedBeforeDatabaseAccess() {
        assertThatThrownBy(() -> service.search(viewer, "字".repeat(51), new PageRequest(1, 20)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("50");
        verifyNoInteractions(store);
    }
}
