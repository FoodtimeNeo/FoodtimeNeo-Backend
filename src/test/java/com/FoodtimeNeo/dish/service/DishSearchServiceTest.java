package com.FoodtimeNeo.dish.service;

import com.FoodtimeNeo.common.api.PageRequest;
import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dish.mapper.DishSearchMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DishSearchServiceTest {
    private static DishSearchService service(DishSearchMapper mapper) {
        return new DishSearchService(mapper);
    }

    private static BusinessException failure(Runnable action) {
        return org.assertj.core.api.Assertions.catchThrowableOfType(BusinessException.class, action::run);
    }

    @Test
    void blankKeywordIsABrowseQueryRatherThanAnEmptyPage() {
        DishSearchMapper mapper = mock(DishSearchMapper.class);
        when(mapper.count(any())).thenReturn(3L);
        when(mapper.search(any(), anyInt(), anyLong())).thenReturn(List.of());
        service(mapper).search(" ", null, null, null, null, null, new PageRequest(1, 20));
        ArgumentCaptor<DishSearchQuery> query = ArgumentCaptor.forClass(DishSearchQuery.class);
        verify(mapper).count(query.capture());
        assertThat(query.getValue().q()).isEmpty();
        assertThat(query.getValue().sort()).isEqualTo("relevance");
    }

    @Test
    void normalizesSortAndDeduplicatesHalls() {
        DishSearchMapper mapper = mock(DishSearchMapper.class);
        when(mapper.count(any())).thenReturn(0L);
        UUID hall = UUID.randomUUID();
        service(mapper).search("鸡腿", List.of(hall, hall), null, null, null, " PRICE_DESC ",
                new PageRequest(1, 20));
        ArgumentCaptor<DishSearchQuery> query = ArgumentCaptor.forClass(DishSearchQuery.class);
        verify(mapper).count(query.capture());
        assertThat(query.getValue().diningHallIds()).containsExactly(hall);
        assertThat(query.getValue().sort()).isEqualTo("price_desc");
        assertThat(query.getValue().q()).isEqualTo("鸡腿");
    }

    @Test
    void rejectsInvalidBoundsAndSortValues() {
        DishSearchService service = service(mock(DishSearchMapper.class));
        assertThat(failure(() -> service.search("x", null, new BigDecimal("-1"), null, null, null,
                new PageRequest(1, 20))).getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(failure(() -> service.search("x", null, new BigDecimal("20"), new BigDecimal("10"),
                null, null, new PageRequest(1, 20))).getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(failure(() -> service.search("x", null, null, null, new BigDecimal("5.1"),
                null, new PageRequest(1, 20))).getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(failure(() -> service.search("x", null, null, null, null, "name",
                new PageRequest(1, 20))).getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(failure(() -> service.search("字".repeat(51), null, null, null, null, null,
                new PageRequest(1, 20))).getCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void acceptsZeroPriceAndZeroRatingAsValidBounds() {
        DishSearchMapper mapper = mock(DishSearchMapper.class);
        when(mapper.count(any())).thenReturn(0L);
        service(mapper).search("", null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "price_asc", new PageRequest(1, 20));
        ArgumentCaptor<DishSearchQuery> query = ArgumentCaptor.forClass(DishSearchQuery.class);
        verify(mapper).count(query.capture());
        assertThat(query.getValue().minPrice()).isEqualByComparingTo("0");
        assertThat(query.getValue().maxPrice()).isEqualByComparingTo("0");
        assertThat(query.getValue().minRating()).isEqualByComparingTo("0");
    }
}
