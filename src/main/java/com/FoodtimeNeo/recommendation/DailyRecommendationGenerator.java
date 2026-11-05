package com.FoodtimeNeo.recommendation;

import com.FoodtimeNeo.config.properties.DailyRecommendationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

import static com.FoodtimeNeo.recommendation.RecommendationModels.*;

/** Pure algorithm entry point shared by production and the offline simulation. */
@Component
public class DailyRecommendationGenerator {
    private final RecommendationWeightCalculator weights;
    private final RecommendationScoreCalculator scores;
    private final RecommendationDiversifier diversifier;

    public DailyRecommendationGenerator(RecommendationWeightCalculator weights,
            RecommendationScoreCalculator scores, RecommendationDiversifier diversifier) {
        this.weights = weights;
        this.scores = scores;
        this.diversifier = diversifier;
    }

    public List<SelectedCandidate> generate(List<Candidate> candidates, double globalAverage,
                                            LocalDate date, DailyRecommendationProperties config) {
        long qualified = candidates.stream().filter(c -> c.reviewCount() >= 5).count();
        Weights dynamic = weights.calculate(candidates.size(), qualified, config);
        var scored = scores.score(candidates, date, config.algorithmVersion(), config.smoothingPrior(),
                dynamic, globalAverage);
        return diversifier.select(scored, weights.qualityQuota(dynamic.coverage(), config.targetSize()),
                config.targetSize(), config.maxPerStall(), config.maxPerHall(), config.minimumHalls(),
                date, config.cooldownDays());
    }
}
