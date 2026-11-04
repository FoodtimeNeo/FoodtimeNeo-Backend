package com.FoodtimeNeo.recommendation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class RecommendationModels {
    private RecommendationModels() { }

    public record Candidate(UUID id, String name, UUID diningHallId, String diningHallName,
                            UUID stallId, String stallName, BigDecimal price, BigDecimal averageRating,
                            long reviewCount, long reviews7d, String imageUrl, long exposure90d,
                            LocalDate lastRecommendedDate) {
        public Candidate withExposure(long count, LocalDate lastDate) {
            return new Candidate(id, name, diningHallId, diningHallName, stallId, stallName, price,
                    averageRating, reviewCount, reviews7d, imageUrl, count, lastDate);
        }
    }

    public record Weights(double quality, double heat, double rotation, double exposure,
                          double coverage, double alpha) {
        public double sum() { return quality + heat + rotation + exposure; }
    }

    public record ScoredCandidate(Candidate candidate, double quality, double heat,
                                  double rotation, double exposure, double score) { }

    public record SelectedCandidate(ScoredCandidate scored, String type, String reasonCode) { }

    public record StoredItem(UUID dishId, int position, String type, double score, String reasonCode) { }
}
