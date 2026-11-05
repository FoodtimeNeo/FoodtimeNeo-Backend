package com.FoodtimeNeo.recommendation;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.List;

@Component
public class RecommendationScoreCalculator {
    public List<RecommendationModels.ScoredCandidate> score(List<RecommendationModels.Candidate> candidates,
                                                             LocalDate date, String algorithmVersion,
                                                             int smoothingPrior, RecommendationModels.Weights weights) {
        long totalReviews = candidates.stream().mapToLong(RecommendationModels.Candidate::reviewCount).sum();
        double globalAverage = totalReviews == 0 ? 3.5d
                : candidates.stream().mapToDouble(c -> c.reviewCount() * safe(c.averageRating(), 3.5d)).sum()
                / totalReviews;
        return score(candidates, date, algorithmVersion, smoothingPrior, weights, globalAverage);
    }

    public List<RecommendationModels.ScoredCandidate> score(List<RecommendationModels.Candidate> candidates,
            LocalDate date, String algorithmVersion, int smoothingPrior, RecommendationModels.Weights weights,
            double globalAverage) {
        long maxRecent = candidates.stream().mapToLong(RecommendationModels.Candidate::reviews7d).max().orElse(0L);
        return candidates.stream().map(c -> {
            double q = bayesianQuality(c.reviewCount(), safe(c.averageRating(), globalAverage), globalAverage,
                    smoothingPrior);
            double h = maxRecent == 0 ? 0d : Math.log1p(c.reviews7d()) / Math.log1p(maxRecent);
            double r = stableHash(date, c.id(), algorithmVersion);
            double e = 1d / (1d + Math.max(0L, c.exposure90d()));
            double score = weights.quality() * q + weights.heat() * h + weights.rotation() * r + weights.exposure() * e;
            return new RecommendationModels.ScoredCandidate(c, finite(q), finite(h), finite(r), finite(e), finite(score));
        }).toList();
    }

    public double bayesianQuality(long reviewCount, double averageRating, double globalAverage, int prior) {
        double n = Math.max(0L, reviewCount);
        double m = Math.max(1, prior);
        double global = isFinite(globalAverage) ? Math.max(1d, Math.min(5d, globalAverage)) : 3.5d;
        double avg = isFinite(averageRating) ? Math.max(1d, Math.min(5d, averageRating)) : global;
        return finite((n * avg + m * global) / (n + m) / 5d);
    }

    public double stableHash(LocalDate date, java.util.UUID dishId, String algorithmVersion) {
        try {
            byte[] input = (date + "\n" + dishId + "\n" + algorithmVersion).getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            // Big endian, high 53 bits; mapping is exact and cannot round to 1.
            long bits = ByteBuffer.wrap(digest, 0, Long.BYTES).getLong() >>> 11;
            return bits * 0x1.0p-53;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 required", e);
        }
    }

    public double confidence(long reviewCount, int prior) {
        if (reviewCount < 0 || prior <= 0) throw new IllegalArgumentException("Invalid review count/prior");
        return reviewCount / ((double) reviewCount + prior);
    }

    private static double safe(java.math.BigDecimal value, double fallback) { return value == null ? fallback : safe(value.doubleValue(), fallback); }
    private static double safe(double value, double fallback) { return isFinite(value) ? value : fallback; }
    private static boolean isFinite(double value) { return Double.isFinite(value); }
    private static double finite(double value) { return isFinite(value) ? value : 0d; }
}
