package com.FoodtimeNeo.feedback;

import com.FoodtimeNeo.user.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.*;
import java.util.UUID;
import static org.mockito.Mockito.*;

class FeedbackServiceTest {
    @Test void dailyQuotaUsesUtcPlusEightMidnightInsteadOfServerLocalZone() {
        UUID id=UUID.randomUUID();UserProfile actor=new UserProfile(id,"test@example.test","用户","visitor","active",Instant.EPOCH);
        for(String time:new String[]{"2026-11-11T15:59:59Z","2026-11-11T16:00:00Z"}) {
            Instant now=Instant.parse(time);FeedbackStore store=mock(FeedbackStore.class);
            var input=new FeedbackDtos.Create("usage","时区检查",UUID.randomUUID());
            when(store.create(eq(id),eq(input),eq(input.content()),eq("device"),eq(true),eq(now)))
                    .thenReturn(new FeedbackDtos.Row(UUID.randomUUID(),input.type(),input.content(),"pending",now,now,"用户",null));
            var service=new FeedbackService(store,Clock.fixed(now,ZoneId.of("America/New_York")),mock(PlatformTransactionManager.class));
            service.create(actor,input,"device");
            Instant start=Instant.parse(time.endsWith("59Z")?"2026-11-10T16:00:00Z":"2026-11-11T16:00:00Z");
            verify(store).ownerCount(id,start,start.plus(Duration.ofDays(1)));
            verify(store).deviceCount("device",start,start.plus(Duration.ofDays(1)));
        }
    }
}
