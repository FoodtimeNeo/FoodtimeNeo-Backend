package com.FoodtimeNeo.dining.service;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.dining.entity.Stall;
import com.FoodtimeNeo.dining.entity.StallImage;
import com.FoodtimeNeo.dining.mapper.StallImageMapper;
import com.FoodtimeNeo.dining.mapper.StallMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StallServiceTest {
    private final StallMapper mapper = mock(StallMapper.class);
    private final StallImageMapper imageMapper = mock(StallImageMapper.class);
    private final StallService service = new StallService(mapper, imageMapper);
    private final UUID hall = UUID.randomUUID();

    @BeforeEach
    void defaultImagesEmpty() {
        when(imageMapper.findActiveByDiningHall(hall)).thenReturn(List.of());
    }

    @Test
    void preservesOrderingAndProjectsOnlyDisplayFields() {
        var one = new Stall(hall, UUID.randomUUID(), "第一档口", "简介", "https://example.test/stall.jpg", "active", 1);
        var two = new Stall(hall, UUID.randomUUID(), "第二档口", null, null, "active", 2);
        when(mapper.findActiveByDiningHall(hall)).thenReturn(List.of(one, two));
        var result = service.listByDiningHall(hall);
        assertThat(result).extracting(row -> row.id()).containsExactly(one.id(), two.id());
        assertThat(result.get(0).diningHallId()).isEqualTo(hall);
        assertThat(result.get(0).coverImageUrl()).isEqualTo("https://example.test/stall.jpg");
        assertThat(result.get(1).description()).isNull();
    }
    @Test
    void activeHallWithoutActiveStallsReturnsEmptyArray() {
        when(mapper.findActiveByDiningHall(hall)).thenReturn(List.of(new Stall(hall, null, null, null, null, null, null)));
        assertThat(service.listByDiningHall(hall)).isEmpty();
    }
    @Test
    void absentOrInactiveHallReturns404() {
        when(mapper.findActiveByDiningHall(hall)).thenReturn(List.of());
        assertThatThrownBy(() -> service.listByDiningHall(hall)).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getCode()).isEqualTo("DINING_HALL_NOT_FOUND");
            assertThat(error.getStatus().value()).isEqualTo(404);
        });
    }
    @Test
    void databaseFailureDoesNotExposeSqlOrConnectionDetails() {
        when(mapper.findActiveByDiningHall(hall)).thenThrow(new DataAccessResourceFailureException("private-sql-host"));
        assertThatThrownBy(() -> service.listByDiningHall(hall)).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getCode()).isEqualTo("STALLS_UNAVAILABLE");
            assertThat(error.getStatus().value()).isEqualTo(503);
        }).hasMessageNotContaining("private-sql-host");
    }

    @Test
    void listAttachesGalleryAndKeepsSortOrder() {
        var stallId = UUID.randomUUID();
        var stall = new Stall(hall, stallId, "面食档口", null, "https://example.test/cover.jpg", "active", 1);
        when(mapper.findActiveByDiningHall(hall)).thenReturn(List.of(stall));
        // Mapper 按 sort_order ASC, id ASC 返回；service 保持该顺序供前端直接轮播。
        var first = new StallImage(stallId, UUID.randomUUID(), "https://example.test/1.jpg", 0);
        var second = new StallImage(stallId, UUID.randomUUID(), "https://example.test/2.jpg", 1);
        when(imageMapper.findActiveByDiningHall(hall)).thenReturn(List.of(first, second));
        var result = service.listByDiningHall(hall);
        assertThat(result.get(0).images()).extracting(image -> image.imageUrl())
                .containsExactly("https://example.test/1.jpg", "https://example.test/2.jpg");
        assertThat(result.get(0).coverImageUrl()).isEqualTo("https://example.test/cover.jpg");
    }

    @Test
    void listWithoutImagesReturnsEmptyGallery() {
        var stall = new Stall(hall, UUID.randomUUID(), "无图档口", null, null, "active", 1);
        when(mapper.findActiveByDiningHall(hall)).thenReturn(List.of(stall));
        var result = service.listByDiningHall(hall);
        assertThat(result.get(0).images()).isEmpty();
    }

    @Test
    void listOnlyKeepsImagesOfVisibleStalls() {
        var visible = new Stall(hall, UUID.randomUUID(), "可见档口", null, null, "active", 1);
        when(mapper.findActiveByDiningHall(hall)).thenReturn(List.of(visible,
                new Stall(hall, null, null, null, null, null, null)));
        var orphan = new StallImage(UUID.randomUUID(), UUID.randomUUID(), "https://example.test/orphan.jpg", 0);
        var mine = new StallImage(visible.id(), UUID.randomUUID(), "https://example.test/mine.jpg", 0);
        when(imageMapper.findActiveByDiningHall(hall)).thenReturn(List.of(orphan, mine));
        var result = service.listByDiningHall(hall);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).images()).extracting(image -> image.imageUrl())
                .containsExactly("https://example.test/mine.jpg");
    }

    @Test
    void detailReturnsGalleryFromFirstImage() {
        var stallId = UUID.randomUUID();
        when(mapper.findVisibleById(stallId))
                .thenReturn(new Stall(hall, stallId, "面食档口", "简介", "https://example.test/cover.jpg", "active", 1));
        var first = new StallImage(stallId, UUID.randomUUID(), "https://example.test/1.jpg", 0);
        var second = new StallImage(stallId, UUID.randomUUID(), "https://example.test/2.jpg", 5);
        when(imageMapper.findByStall(stallId)).thenReturn(List.of(first, second));
        var result = service.getById(stallId);
        assertThat(result.id()).isEqualTo(stallId);
        assertThat(result.images()).extracting(image -> image.imageUrl())
                .containsExactly("https://example.test/1.jpg", "https://example.test/2.jpg");
    }

    @Test
    void detailWithoutImagesReturnsEmptyGallery() {
        var stallId = UUID.randomUUID();
        when(mapper.findVisibleById(stallId))
                .thenReturn(new Stall(hall, stallId, "无图档口", null, null, "active", 1));
        when(imageMapper.findByStall(stallId)).thenReturn(List.of());
        assertThat(service.getById(stallId).images()).isEmpty();
    }

    @Test
    void absentOrInactiveStallDetailReturns404() {
        var stallId = UUID.randomUUID();
        when(mapper.findVisibleById(stallId)).thenReturn(null);
        assertThatThrownBy(() -> service.getById(stallId)).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getCode()).isEqualTo("STALL_NOT_FOUND");
            assertThat(error.getStatus().value()).isEqualTo(404);
        });
        verifyNoInteractions(imageMapper);
    }

    @Test
    void detailDatabaseFailureReturns503WithoutInternalDetails() {
        var stallId = UUID.randomUUID();
        when(mapper.findVisibleById(stallId)).thenThrow(new DataAccessResourceFailureException("private-sql-host"));
        assertThatThrownBy(() -> service.getById(stallId)).isInstanceOfSatisfying(BusinessException.class, error -> {
            assertThat(error.getCode()).isEqualTo("STALLS_UNAVAILABLE");
            assertThat(error.getStatus().value()).isEqualTo(503);
        }).hasMessageNotContaining("private-sql-host");
    }
}
