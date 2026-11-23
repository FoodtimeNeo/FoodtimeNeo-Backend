package com.FoodtimeNeo.moderation;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.ai.mapper.AiModerationStore;
import com.FoodtimeNeo.community.mapper.CommunityStore;
import com.FoodtimeNeo.community.service.CommunityNotificationService;
import com.FoodtimeNeo.config.properties.AiTriageProperties;
import com.FoodtimeNeo.moderation.dto.ModerationItem;
import com.FoodtimeNeo.moderation.mapper.ModerationStore;
import com.FoodtimeNeo.moderation.service.ModerationService;
import com.FoodtimeNeo.review.mapper.PublishedReviewMapper;
import com.FoodtimeNeo.review.service.ReviewAccessService;
import com.FoodtimeNeo.submission.mapper.CatalogSubmissionMapper;
import com.FoodtimeNeo.venue.VenueReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReviewPhotoAiDecisionTest {
    private final ModerationStore store=mock(ModerationStore.class);
    private final AiModerationStore ai=mock(AiModerationStore.class);
    private final PublishedReviewMapper reviews=mock(PublishedReviewMapper.class);
    private final AiTriageProperties properties=mock(AiTriageProperties.class);
    private final ModerationService service=new ModerationService(store,reviews,mock(CatalogSubmissionMapper.class),
            mock(CommunityStore.class),mock(ReviewAccessService.class),ai,properties,mock(VenueReviewService.class),mock(CommunityNotificationService.class));
    private final UUID id=UUID.randomUUID(),dish=UUID.randomUUID();

    private void pending() {
        ModerationItem item=mock(ModerationItem.class);
        when(item.id()).thenReturn(id);when(item.type()).thenReturn("photo");when(item.targetId()).thenReturn(dish);
        when(item.status()).thenReturn("pending");when(store.lock("photo",id)).thenReturn(item);
        when(store.isReviewPhoto(id)).thenReturn(true);
        when(ai.insert(any(),eq("photo"),eq(id),any(),any(),any(),any(),any())).thenReturn(1);
        when(reviews.lockVisibleDish(dish)).thenReturn(dish);
        when(properties.autoRejectAbove()).thenReturn(0.9);
    }

    @Test void lowVerdictNeedsExplicitThresholdAndVeryHighConfidence() {
        pending();
        ReflectionTestUtils.setField(service,"reviewPhotoThreshold",1.01);
        service.recordReviewPhotoVerdict(id,new AiVerdict("LOW",1,List.of(),"匹配","vision"));
        verify(store,never()).complete(any(),any(),any(),any(),any(),any(),any());
        ReflectionTestUtils.setField(service,"reviewPhotoThreshold",0.99);
        service.recordReviewPhotoVerdict(id,new AiVerdict("LOW",0.98,List.of(),"待确认","vision"));
        verify(store,never()).complete(any(),any(),any(),any(),any(),any(),any());
        service.recordReviewPhotoVerdict(id,new AiVerdict("LOW",0.99,List.of(),"匹配","vision"));
        verify(store).complete(any(),isNull(),eq("vision"),eq("approved"),eq("匹配"),eq(id),isNull());
    }

    @Test void unrelatedUncertainImageDoesNotAutoApprove() {
        pending();ReflectionTestUtils.setField(service,"reviewPhotoThreshold",0.99);
        service.recordReviewPhotoVerdict(id,new AiVerdict("MEDIUM",1,List.of(),"无法确认","vision"));
        verify(store,never()).complete(any(),any(),any(),any(),any(),any(),any());
    }
}
