package com.FoodtimeNeo.security;

import com.FoodtimeNeo.authz.CapabilityService;
import com.FoodtimeNeo.catalog.cache.CatalogCacheConfiguration;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.correction.CatalogCorrectionDtos;
import com.FoodtimeNeo.correction.CatalogCorrectionService;
import com.FoodtimeNeo.correction.CatalogCorrectionStore;
import com.FoodtimeNeo.dish.service.DishSearchService;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.notification.NotificationPublisher;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SecurityAuditBatchABTest {

    @Test
    void extremeScientificBigDecimalPriceIsRejectedWithoutOverflowOrOom() {
        var service = new CatalogCorrectionService(
                mock(CatalogCorrectionStore.class),
                mock(ImageStorageService.class),
                mock(CapabilityService.class),
                mock(NotificationPublisher.class),
                Clock.systemUTC());
        var actor = new UserProfile(UUID.randomUUID(), "123@bjtu.edu.cn", "测试", "user", "active", null);
        var request = new CatalogCorrectionDtos.CreateRequest(
                "dish", UUID.randomUUID(), 1L, List.of("price"),
                null, null, new BigDecimal("1e2147483647"), "核对价格",
                List.of(), UUID.randomUUID().toString(), null);
        assertThatThrownBy(() -> service.submit(actor, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("价格");
    }

    @Test
    @SuppressWarnings("unchecked")
    void catalogQueryAdvisorRunsStrictlyBeforeSpringTransactionalAdvisor() {
        var advisor = CatalogCacheConfiguration.catalogQueryAdvisor(
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class));
        assertThat(advisor.getOrder()).isLessThan(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void dishSearchRejectsExcessiveHallIdsNullEntriesAndExtremePrices() {
        List<UUID> tooMany = new ArrayList<>();
        for (int i = 0; i < DishSearchService.MAX_DINING_HALL_IDS + 1; i++) {
            tooMany.add(UUID.randomUUID());
        }
        assertThatThrownBy(() -> DishSearchService.normalize("", tooMany, null, null, null, "relevance"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("50");

        List<UUID> withNull = new ArrayList<>();
        withNull.add(null);
        assertThatThrownBy(() -> DishSearchService.normalize("", withNull, null, null, null, "relevance"))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> DishSearchService.normalize("", List.of(),
                new BigDecimal("1e2147483647"), null, null, "relevance"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("价格");
    }
}
