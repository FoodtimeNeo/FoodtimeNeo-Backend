package com.FoodtimeNeo.recommendation;

import com.FoodtimeNeo.config.properties.DailyRecommendationProperties;
import org.springframework.stereotype.Component;

@Component
public class RecommendationWeightCalculator {
    public RecommendationModels.Weights calculate(long eligible, long qualified) {
        return calculate(eligible, qualified, DailyRecommendationProperties.defaults());
    }

    public RecommendationModels.Weights calculate(long eligible, long qualified, DailyRecommendationProperties config) {
        if (eligible < 0 || qualified < 0 || qualified > eligible) {
            throw new IllegalArgumentException("Invalid recommendation maturity counts");
        }
        double coverage = eligible == 0 ? 0 : (double) qualified / eligible;
        double alpha = Math.min(1, coverage / .5);
        var cold = config.coldWeights();
        var mature = config.matureWeights();
        return new RecommendationModels.Weights(lerp(cold.quality(), mature.quality(), alpha),
                lerp(cold.heat(), mature.heat(), alpha), lerp(cold.rotation(), mature.rotation(), alpha),
                lerp(cold.exposure(), mature.exposure(), alpha), coverage, alpha);
    }

    public int qualityQuota(double coverage, int targetSize) {
        return Math.min((int) Math.floor(targetSize * .7), (int) Math.floor(coverage * targetSize * 1.5));
    }

    private static double lerp(double cold, double mature, double alpha) {
        return cold + (mature - cold) * alpha;
    }
}
