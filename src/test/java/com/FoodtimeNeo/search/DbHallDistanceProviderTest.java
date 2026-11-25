package com.FoodtimeNeo.search;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dining.entity.DiningHall;
import com.FoodtimeNeo.dining.mapper.DiningHallMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DbHallDistanceProviderTest {
    @Test
    void readsActiveHallCoordinatesAndAppliesConfigOverrides() {
        UUID liuyuan = UUID.fromString("68166b66-14d1-58c5-960d-572228254c20");
        UUID minghu = UUID.fromString("74ed9519-f3e3-5ce8-a89c-a353e2892edd");
        UUID unlocated = UUID.randomUUID();
        UUID configOnly = UUID.randomUUID();

        DiningHallMapper mapper = mock(DiningHallMapper.class);
        when(mapper.findActive()).thenReturn(List.of(
                new DiningHall(liuyuan, "留园食堂", null, null, new BigDecimal("39.9488990"), new BigDecimal("116.3374050"), "active", 0, null, 0, 1),
                new DiningHall(minghu, "明湖食堂", null, null, new BigDecimal("39.9518280"), new BigDecimal("116.3371990"), "active", 1, null, 0, 1),
                new DiningHall(unlocated, "待补充食堂", null, null, null, null, "active", 2, null, 0, 1)
        ));

        SearchLocations overrides = new SearchLocations(Map.of(
                configOnly, new SearchLocations.Point(39.9500, 116.3350, "override-test")
        ));
        var provider = new DbHallDistanceProvider(mapper, overrides);

        assertThat(provider.hallIds()).containsExactlyInAnyOrder(liuyuan, minghu, configOnly);
        assertThat(provider.distances(null, null)).isEmpty();

        var distances = provider.distances(39.9488990, 116.3374050);
        assertThat(distances.get(liuyuan)).isCloseTo(0d, within(0.5));
        assertThat(distances.get(minghu)).isBetween(300d, 360d);
        assertThat(distances).containsKey(configOnly).doesNotContainKey(unlocated);

        assertThatThrownBy(() -> provider.distances(39.95, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> provider.distances(95.0, 116.33)).isInstanceOf(BusinessException.class);
    }
}
