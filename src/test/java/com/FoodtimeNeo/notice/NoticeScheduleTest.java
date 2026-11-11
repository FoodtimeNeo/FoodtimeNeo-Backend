package com.FoodtimeNeo.notice;

import com.FoodtimeNeo.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

class NoticeScheduleTest {
    private final Instant now=Instant.parse("2026-11-11T07:30:00Z");
    @Test void omittedDisplayStartUsesPublicationAndDateDeadlineIncludesThatUtcPlusEightDay() {
        var schedule=NoticeSchedule.resolve(new NoticeDtos.Schedule(now,null,null,LocalDate.of(2026,11,11),true,true,true),now);
        assertThat(schedule.startsAt()).isEqualTo(now);
        assertThat(schedule.endsAt()).isEqualTo(Instant.parse("2026-11-11T16:00:00Z"));
        assertThat(schedule.dateOnly()).isTrue();
    }
    @Test void explicitMinuteDeadlineUsesAbsoluteInstantWithoutSystemTimezone() {
        var end=Instant.parse("2026-11-11T08:01:00Z");
        var schedule=NoticeSchedule.resolve(new NoticeDtos.Schedule(now,null,end,null,false,true,true),now);
        assertThat(schedule.endsAt()).isEqualTo(end);assertThat(schedule.dateOnly()).isFalse();
    }
    @Test void rejectsReversedIntervalsAmbiguousDeadlinesAndEnabledDrafts() {
        for(var input:new NoticeDtos.Schedule[]{
                new NoticeDtos.Schedule(now,now.minusSeconds(1),null,null,false,true,true),
                new NoticeDtos.Schedule(now,now,now,null,false,true,true),
                new NoticeDtos.Schedule(now,null,now.plusSeconds(60),LocalDate.of(2026,11,11),false,true,true),
                new NoticeDtos.Schedule(now,null,null,null,false,false,true)})
            assertThatThrownBy(()->NoticeSchedule.resolve(input,now)).isInstanceOf(BusinessException.class);
    }
}
