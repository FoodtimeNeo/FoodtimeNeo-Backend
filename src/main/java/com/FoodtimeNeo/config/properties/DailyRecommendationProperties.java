package com.FoodtimeNeo.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.scheduling.support.CronExpression;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@ConfigurationProperties(prefix = "app.recommendation.daily")
public record DailyRecommendationProperties(
        @DefaultValue("FT-HybridRec-v2") String algorithmVersion,
        @DefaultValue("10") int targetSize,
        @DefaultValue("30") int cooldownDays,
        @DefaultValue("90") int historyDays,
        @DefaultValue("7") int recentDays,
        @DefaultValue("10") int smoothingPrior,
        @DefaultValue("2") int maxPerStall,
        @DefaultValue("4") int maxPerHall,
        @DefaultValue("3") int minimumHalls,
        @DefaultValue("P2D") Duration cacheTtl,
        @DefaultValue("0 5 0 * * *") String cron,
        @DefaultValue("Asia/Shanghai") String zone,
        WeightProfile coldWeights,
        WeightProfile matureWeights) {

    public DailyRecommendationProperties {
        if (algorithmVersion == null || algorithmVersion.isBlank() || algorithmVersion.length() > 64
                || targetSize <= 0 || cooldownDays < 0 || historyDays <= 0 || recentDays <= 0
                || smoothingPrior <= 0 || maxPerStall <= 0 || maxPerHall <= 0 || minimumHalls < 0
                || cacheTtl == null || cacheTtl.compareTo(Duration.ofDays(2)) < 0
                || !"Asia/Shanghai".equals(zone) || !CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("Invalid daily recommendation configuration");
        }
        if (coldWeights == null) coldWeights = new WeightProfile(.10, .05, .55, .30);
        if (matureWeights == null) matureWeights = new WeightProfile(.40, .15, .25, .20);
    }

    public static DailyRecommendationProperties defaults() {
        return new DailyRecommendationProperties("FT-HybridRec-v2", 10, 30, 90, 7, 10,
                2, 4, 3, Duration.ofDays(2), "0 5 0 * * *", "Asia/Shanghai", null, null);
    }

    public String configHash() {
        String value = String.join("|", algorithmVersion, Integer.toString(targetSize),
                Integer.toString(cooldownDays), Integer.toString(historyDays), Integer.toString(recentDays),
                Integer.toString(smoothingPrior), Integer.toString(maxPerStall), Integer.toString(maxPerHall),
                Integer.toString(minimumHalls), cacheTtl.toString(), cron, zone,
                coldWeights.toString(), matureWeights.toString());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 required", e);
        }
    }

    public record WeightProfile(double quality, double heat, double rotation, double exposure) {
        public WeightProfile {
            double sum = quality + heat + rotation + exposure;
            if (!Double.isFinite(sum) || quality < 0 || heat < 0 || rotation < 0 || exposure < 0
                    || Math.abs(sum - 1) > 1e-12) {
                throw new IllegalArgumentException("Recommendation weights must be finite, nonnegative and sum to one");
            }
        }
    }
}
