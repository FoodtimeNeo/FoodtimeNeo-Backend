package com.FoodtimeNeo.media;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.media.dto.ImageSubmissionRequest;
import com.FoodtimeNeo.media.dto.ImageSubmissionResponse;
import com.FoodtimeNeo.media.mapper.ImageSubmissionStore;
import com.FoodtimeNeo.media.service.ImageStorageService;
import com.FoodtimeNeo.media.service.ImageSubmissionService;
import com.FoodtimeNeo.review.mapper.PublishedReviewMapper;
import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewPhotoSubmissionTest {
    private final ImageSubmissionStore store=mock(ImageSubmissionStore.class);
    private final ImageStorageService images=mock(ImageStorageService.class);
    private final PublishedReviewMapper catalog=mock(PublishedReviewMapper.class);
    private final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    private final ImageSubmissionService service=new ImageSubmissionService(store,images,catalog,events);
    private final UUID owner=UUID.randomUUID(),dish=UUID.randomUUID(),review=UUID.randomUUID();
    private final UserProfile actor=new UserProfile(owner,"test@example.test","test","user","active",null);

    @Test void rejectsReviewOwnedByAnotherUserOrDish() {
        when(catalog.lockVisibleDish(dish)).thenReturn(dish);
        assertThatThrownBy(()->service.submit(actor,dish,new ImageSubmissionRequest("image",review)))
                .isInstanceOf(BusinessException.class);
        verify(store,never()).create(any(),any(),any(),any());
    }

    @Test void retryReturnsSameSubmissionAndDoesNotRepeatEvent() {
        when(catalog.lockVisibleDish(dish)).thenReturn(dish);
        when(store.ownsReview(review,dish,owner)).thenReturn(true);
        var existing=new ImageSubmissionResponse(UUID.randomUUID(),dish,"dish",null,"",null,"","image","pending",null,null,null);
        when(store.forReview(review)).thenReturn(Optional.of(existing));
        assertThat(service.submit(actor,dish,new ImageSubmissionRequest("image",review))).isEqualTo(existing);
        verifyNoInteractions(events);
        assertThatThrownBy(()->service.submit(actor,dish,new ImageSubmissionRequest("different",review)))
                .isInstanceOf(BusinessException.class);
    }
}
