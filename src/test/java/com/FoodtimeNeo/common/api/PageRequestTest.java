package com.FoodtimeNeo.common.api;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PageRequestTest {
    @Test
    void maximumPageUsesLongOffsetWithoutIntegerOverflow() {
        assertThat(new PageRequest(Integer.MAX_VALUE, 100).offset()).isEqualTo(214748364600L);
    }
}
