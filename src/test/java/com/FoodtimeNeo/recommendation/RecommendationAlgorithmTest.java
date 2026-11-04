package com.FoodtimeNeo.recommendation;

import com.FoodtimeNeo.config.properties.DailyRecommendationProperties;
import com.FoodtimeNeo.config.properties.DailyRecommendationProperties.WeightProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.FoodtimeNeo.recommendation.RecommendationModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Offset.offset;

class RecommendationAlgorithmTest {
    final RecommendationScoreCalculator scores = new RecommendationScoreCalculator();
    final RecommendationWeightCalculator weights = new RecommendationWeightCalculator();
    final RecommendationDiversifier diversifier = new RecommendationDiversifier();
    final DailyRecommendationGenerator generator = new DailyRecommendationGenerator(weights, scores, diversifier);
    final DailyRecommendationProperties config = DailyRecommendationProperties.defaults();
    final LocalDate date = LocalDate.of(2026, 11, 3);

    static UUID id(String text) { return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)); }
    static Candidate candidate(int i, long reviews) {
        return new Candidate(id("dish-" + i), "dish" + i, id("hall-" + i % 6), "hall",
                id("stall-" + i % 36), "stall", BigDecimal.TEN,
                reviews == 0 ? null : BigDecimal.valueOf(4), reviews, 0, null, 0, null);
    }
    List<Candidate> catalogue(int size, long reviews) {
        List<Candidate> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add(candidate(i, reviews));
        return list;
    }
    List<SelectedCandidate> generate(List<Candidate> list) { return generator.generate(list, 3.5, date, config); }
    List<ScoredCandidate> scored(List<Candidate> list) {
        return scores.score(list, date, config.algorithmVersion(), 10, weights.calculate(list.size(), 0));
    }

    @Test void noReviewsUsePrior() {
        assertThat(scores.bayesianQuality(0, Double.NaN, 3.5, 10)).isEqualTo(.7);
    }
    @Test void singleExtremeReviewIsSmoothed() {
        assertThat(scores.bayesianQuality(1, 5, 3.5, 10)).isBetween(.70, .75);
        assertThat(scores.bayesianQuality(1, 1, 3.5, 10)).isGreaterThan(.6);
    }
    @Test void manyReviewsApproachObservedAverage() {
        assertThat(scores.bayesianQuality(1000, 5, 3.5, 10)).isGreaterThan(.99).isLessThan(1);
        assertThat(scores.bayesianQuality(30, 5, 3.5, 10))
                .isGreaterThan(scores.bayesianQuality(1, 5, 3.5, 10));
    }
    @Test void confidenceDescribesReliabilityOnly() {
        assertThat(scores.confidence(0, 10)).isZero();
        assertThat(scores.confidence(1, 10)).isCloseTo(1d / 11, offset(1e-12));
        assertThat(scores.confidence(10, 10)).isEqualTo(.5);
        assertThat(scores.confidence(30, 10)).isEqualTo(.75);
    }
    @Test void zeroHeatAndExposureCompensation() {
        var list = scores.score(List.of(candidate(1, 0), candidate(2, 0).withExposure(4, null)),
                date, config.algorithmVersion(), 10, weights.calculate(2, 0));
        assertThat(list).allMatch(c -> c.heat() == 0 && Double.isFinite(c.score()));
        assertThat(list.get(0).exposure()).isEqualTo(1);
        assertThat(list.get(1).exposure()).isEqualTo(.2);
    }
    @Test void logarithmicHeatIsNormalized() {
        Candidate base = candidate(2, 10);
        Candidate hot = new Candidate(base.id(), base.name(), base.diningHallId(), base.diningHallName(),
                base.stallId(), base.stallName(), base.price(), base.averageRating(), 10, 10, null, 0, null);
        var list = scores.score(List.of(candidate(1, 0), hot), date, config.algorithmVersion(), 10, weights.calculate(2, 1));
        assertThat(list.get(0).heat()).isZero();
        assertThat(list.get(1).heat()).isEqualTo(1);
    }
    @Test void stableHashIsDeterministicDateAndVersionSensitive() {
        double first = scores.stableHash(date, id("x"), config.algorithmVersion());
        assertThat(first).isEqualTo(scores.stableHash(date, id("x"), config.algorithmVersion()));
        assertThat(first).isNotEqualTo(scores.stableHash(date.plusDays(1), id("x"), config.algorithmVersion()));
        assertThat(first).isNotEqualTo(scores.stableHash(date, id("x"), "next-version"));
        for (int i = 0; i < 5000; i++)
            assertThat(scores.stableHash(date, id("hash" + i), config.algorithmVersion())).isGreaterThanOrEqualTo(0).isLessThan(1);
    }
    @Test void coldWeights() {
        assertThat(weights.calculate(1200, 0)).isEqualTo(new Weights(.1, .05, .55, .3, 0, 0));
        assertThat(weights.calculate(0, 0).coverage()).isZero();
    }
    @Test void matureWeights() {
        var result = weights.calculate(1200, 600);
        assertThat(result.quality()).isCloseTo(.4, offset(1e-12));
        assertThat(result.heat()).isCloseTo(.15, offset(1e-12));
        assertThat(result.rotation()).isCloseTo(.25, offset(1e-12));
        assertThat(result.exposure()).isCloseTo(.2, offset(1e-12));
    }
    @Test void weightsInterpolateAndSumToOneAtEveryCoverage() {
        assertThat(weights.calculate(100, 25).alpha()).isEqualTo(.5);
        for (int i = 0; i <= 1000; i++) {
            var result = weights.calculate(1000, i);
            assertThat(result.sum()).isCloseTo(1, offset(1e-12));
            assertThat(List.of(result.quality(), result.heat(), result.rotation(), result.exposure()))
                    .allMatch(v -> Double.isFinite(v) && v >= 0 && v <= 1);
        }
    }
    @Test void invalidWeightsAndCountsAreRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new WeightProfile(-.1, .2, .5, .4));
        assertThatIllegalArgumentException().isThrownBy(() -> new WeightProfile(Double.NaN, 0, 0, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> new WeightProfile(0, 0, 0, Double.POSITIVE_INFINITY));
        assertThatIllegalArgumentException().isThrownBy(() -> new WeightProfile(.2, .2, .2, .2));
        assertThatIllegalArgumentException().isThrownBy(() -> weights.calculate(1, 2));
    }
    @Test void configurationHashTracksWeightsAndRejectsUnsafeSettings() {
        assertThat(config.configHash()).hasSize(64).isEqualTo(DailyRecommendationProperties.defaults().configHash());
        var changed = new DailyRecommendationProperties(config.algorithmVersion(), 10, 30, 90, 7, 10,
                2, 4, 3, config.cacheTtl(), config.cron(), config.zone(), config.matureWeights(), config.matureWeights());
        assertThat(changed.configHash()).isNotEqualTo(config.configHash());
        assertThatIllegalArgumentException().isThrownBy(() -> new DailyRecommendationProperties("v2", 10, 30, 90, 7, 10,
                2, 4, 3, java.time.Duration.ofHours(1), config.cron(), config.zone(), null, null));
    }
    @Test void quotaChangesWithMaturityAndTarget() {
        assertThat(weights.qualityQuota(0, 10)).isZero();
        assertThat(weights.qualityQuota(.2, 10)).isEqualTo(3);
        assertThat(weights.qualityQuota(.5, 10)).isEqualTo(7);
        assertThat(weights.qualityQuota(1, 20)).isEqualTo(14);
    }
    @Test void zeroReviewsStillGenerateTenExploreItems() {
        assertThat(generate(catalogue(1200, 0))).hasSize(10).allMatch(c -> c.type().equals("EXPLORE"));
    }
    @Test void emptyAndSmallCatalogueReturnActualDistinctSize() {
        assertThat(generate(List.of())).isEmpty();
        var result = generate(catalogue(4, 0));
        assertThat(result).hasSize(4);
        assertThat(result.stream().map(c -> c.scored().candidate().id()).distinct()).hasSize(4);
    }
    @Test void overlappingPoolsAreDeduplicatedAndTypeMatchesStage() {
        var result = diversifier.select(scored(catalogue(30, 2)), 7, 10, 2, 4);
        assertThat(result).hasSize(10);
        assertThat(result.stream().map(c -> c.scored().candidate().id()).distinct()).hasSize(10);
        assertThat(result.stream().filter(c -> c.type().equals("QUALITY"))).hasSize(7);
        assertThat(result.stream().filter(c -> c.type().equals("EXPLORE"))).hasSize(3);
    }
    @Test void poolShortageTransfersQuota() {
        var result = diversifier.select(scored(catalogue(30, 0)), 7, 10, 2, 4);
        assertThat(result).hasSize(10).allMatch(c -> c.type().equals("EXPLORE"));
        var qualityOnly = catalogue(30, 10).stream().map(c -> c.withExposure(5, null)).toList();
        assertThat(diversifier.select(scored(qualityOnly), 7, 10, 2, 4))
                .hasSize(10).allMatch(c -> c.type().equals("QUALITY"));
    }
    @Test void cooldownBoundaryIsInclusiveAndOlderDishIsEligible() {
        var list = catalogue(20, 0).stream().map(c -> c.withExposure(1, date.minusDays(30))).collect(Collectors.toCollection(ArrayList::new));
        list.add(candidate(100, 0).withExposure(1, date.minusDays(31)));
        var result = generate(list);
        assertThat(result.get(0).scored().candidate().id()).isEqualTo(candidate(100, 0).id());
    }
    @Test void sufficientNonCoolingCandidatesPreventAnyCooldownRepeat() {
        var list = catalogue(60, 0);
        list.addAll(catalogue(30, 0).stream().map(c -> candidate(1000 + list.indexOf(c), 5)
                .withExposure(1, date.minusDays(1))).toList());
        assertThat(generate(list)).hasSize(10).allMatch(c -> c.scored().candidate().lastRecommendedDate() == null);
    }
    @Test void qualityQuotaNeverForcesCoolingQualityDish() {
        var list = catalogue(30, 0);
        for (int i = 100; i < 140; i++) list.add(candidate(i, 10).withExposure(1, date.minusDays(1)));
        assertThat(generate(list)).hasSize(10).allMatch(c -> c.scored().candidate().reviewCount() == 0);
    }
    @Test void diversityRelaxesBeforeCooldown() {
        UUID hall = id("only-hall"), stall = id("only-stall");
        var list = new ArrayList<Candidate>();
        for (int i = 0; i < 10; i++) {
            var c = candidate(i, 0);
            list.add(new Candidate(c.id(), c.name(), hall, "hall", stall, "stall", c.price(), null, 0, 0, null, 0, null));
        }
        list.add(candidate(100, 10).withExposure(0, date.minusDays(1)));
        assertThat(generate(list)).hasSize(10).allMatch(c -> c.scored().candidate().lastRecommendedDate() == null);
    }
    @Test void coolingFallbackChoosesOldestBeforeScore() {
        var list = new ArrayList<Candidate>();
        for (int i = 1; i <= 15; i++) list.add(candidate(i, 0).withExposure(1, date.minusDays(i)));
        var result = generate(list);
        assertThat(result.stream().map(c -> c.scored().candidate().lastRecommendedDate()))
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(6, 15)
                        .mapToObj(i -> date.minusDays(21 - i)).toList());
    }
    @Test void defaultDiversityAndCoverageHoldWhenFeasible() {
        var result = generate(catalogue(1200, 0));
        assertThat(result.stream().collect(Collectors.groupingBy(c -> c.scored().candidate().stallId(), Collectors.counting())).values())
                .allMatch(n -> n <= 2);
        assertThat(result.stream().collect(Collectors.groupingBy(c -> c.scored().candidate().diningHallId(), Collectors.counting())).values())
                .allMatch(n -> n <= 4);
        assertThat(result.stream().map(c -> c.scored().candidate().diningHallId()).distinct().count()).isGreaterThanOrEqualTo(3);
    }
    @Test void shuffledInputProducesSameOutput() {
        var list = catalogue(1200, 2);
        var first = generate(list);
        Collections.shuffle(list, new Random(23));
        assertThat(generate(list)).isEqualTo(first);
    }
}
