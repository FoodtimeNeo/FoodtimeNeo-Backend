package com.FoodtimeNeo.catalog.cache;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectionCacheTest {
    @Test void oldConcurrentSnapshotCannotReplaceCurrentGenerationAndBudgetEvictsOldest() {
        var cache = new CatalogCacheConfiguration.ProjectionCache();
        cache.put(1, List.of("old"), "old", 1);
        assertThat(cache.get(2, List.of("old"))).isNull();
        cache.put(2, List.of("new"), "new", 1);
        cache.put(1, List.of("old"), "stale", 1);
        assertThat(cache.get(2, List.of("old"))).isNull();
        assertThat(cache.get(2, List.of("new"))).isEqualTo("new");
        for (int i=0; i<260; i++) cache.put(2,List.of(i),i,100);
        assertThat(cache.get(2,List.of(0))).isNull();
        assertThat(cache.get(2,List.of(259))).isEqualTo(259);
    }
}
