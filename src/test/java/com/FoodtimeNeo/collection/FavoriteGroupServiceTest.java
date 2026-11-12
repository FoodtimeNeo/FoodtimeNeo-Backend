package com.FoodtimeNeo.collection;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FavoriteGroupServiceTest {
    @Test
    void namesAreTrimmedAndNormalizedAndCountedAsCodePoints() {
        assertThat(FavoriteGroupService.name("  学习  ")).isEqualTo("学习");
        String twenty = "😀".repeat(20);
        assertThat(FavoriteGroupService.name(twenty)).isEqualTo(twenty);
        assertThatThrownBy(() -> FavoriteGroupService.name("😀".repeat(21)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getCode())
                .isEqualTo("VALIDATION_ERROR");
        assertThatThrownBy(() -> FavoriteGroupService.name("   "))
                .isInstanceOf(BusinessException.class);
    }
}
