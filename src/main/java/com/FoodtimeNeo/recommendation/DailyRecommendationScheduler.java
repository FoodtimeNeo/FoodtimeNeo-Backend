package com.FoodtimeNeo.recommendation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;


@Component
@ConditionalOnProperty(prefix="app.recommendation.daily",name="scheduling-enabled",havingValue="true",matchIfMissing=true)
public class DailyRecommendationScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(DailyRecommendationScheduler.class);
    private final DailyRecommendationService service;

    public DailyRecommendationScheduler(DailyRecommendationService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.recommendation.daily.cron:0 5 0 * * *}", zone = "${app.recommendation.daily.zone:Asia/Shanghai}")
    public void generate() {
        var date = service.currentDate();
        try { service.generate(date); }
        catch (RuntimeException exception) { LOG.error("Daily recommendation generation failed date={}", date, exception); }
    }
}
