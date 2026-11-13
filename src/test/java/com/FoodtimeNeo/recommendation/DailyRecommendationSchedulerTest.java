package com.FoodtimeNeo.recommendation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DailyRecommendationSchedulerTest {
    private final DailyRecommendationService service=mock(DailyRecommendationService.class);
    private final ApplicationContextRunner context=new ApplicationContextRunner()
            .withBean(DailyRecommendationService.class,()->service)
            .withUserConfiguration(DailyRecommendationScheduler.class);
    @Test void productionDefaultRegistersTheJobAndUsesTheBusinessDate() {
        var date=LocalDate.of(2026,11,14);when(service.currentDate()).thenReturn(date);
        context.run(app->{
            assertThat(app).hasSingleBean(DailyRecommendationScheduler.class);
            app.getBean(DailyRecommendationScheduler.class).generate();
            verify(service).generate(date);
        });
    }
    @Test void disposableIntegrationContextsMayDisableAutomaticScheduling() {
        context.withPropertyValues("app.recommendation.daily.scheduling-enabled=false")
                .run(app->assertThat(app).doesNotHaveBean(DailyRecommendationScheduler.class));
        verifyNoInteractions(service);
    }
}
