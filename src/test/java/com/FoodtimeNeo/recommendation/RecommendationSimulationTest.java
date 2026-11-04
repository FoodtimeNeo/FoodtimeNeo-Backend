package com.FoodtimeNeo.recommendation;

import com.FoodtimeNeo.config.properties.DailyRecommendationProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.FoodtimeNeo.recommendation.RecommendationModels.*;
import static org.assertj.core.api.Assertions.assertThat;

class RecommendationSimulationTest {
    @Test
    void ninetyDaysUseProductionAlgorithmAndRealHistory() throws Exception {
        var algorithm = new RecommendationAlgorithmTest();
        var config = DailyRecommendationProperties.defaults();
        List<Candidate> catalogue = new ArrayList<>();
        // 75% unrated, 20% with 1-3, 4% with 5-10, 1% with 30-49 reviews.
        for (int i = 0; i < 1200; i++) {
            int bucket = i % 100;
            long reviews = bucket < 75 ? 0 : bucket < 95 ? 1 + i % 3 : bucket < 99 ? 5 + i % 6 : 30 + i % 20;
            var c = RecommendationAlgorithmTest.candidate(i, reviews);
            catalogue.add(new Candidate(c.id(), c.name(), c.diningHallId(), c.diningHallName(),
                    c.stallId(), c.stallName(), c.price(), c.averageRating(), reviews,
                    reviews == 0 ? 0 : Math.min(reviews, 1 + i % 4), null, 0, null));
        }
        Map<UUID, List<LocalDate>> history = new HashMap<>();
        List<Map<String, Object>> days = new ArrayList<>();
        long repeated = 0, unrated = 0, quality = 0;
        double globalAverage = 4; // Every rated fixture has rating 4, so this is its exact weighted mean.
        LocalDate start = LocalDate.of(2026, 1, 1);
        for (int day = 0; day < 90; day++) {
            LocalDate date = start.plusDays(day);
            var candidates = catalogue.stream().map(c -> {
                var dates = history.getOrDefault(c.id(), List.of());
                long count = dates.stream().filter(d -> !d.isBefore(date.minusDays(90)) && d.isBefore(date)).count();
                return c.withExposure(count, dates.isEmpty() ? null : dates.getLast());
            }).toList();
            var result = algorithm.generator.generate(candidates, globalAverage, date, config);
            assertThat(result).hasSize(10);
            assertThat(result.stream().map(c -> c.scored().candidate().id()).distinct()).hasSize(10);
            for (var item : result) {
                var c = item.scored().candidate();
                if (c.lastRecommendedDate() != null && !c.lastRecommendedDate().isBefore(date.minusDays(30))) repeated++;
                if (c.reviewCount() == 0) unrated++;
                if (item.type().equals("QUALITY")) quality++;
                history.computeIfAbsent(c.id(), ignored -> new ArrayList<>()).add(date);
            }
            long halls = result.stream().map(c -> c.scored().candidate().diningHallId()).distinct().count();
            long stalls = result.stream().map(c -> c.scored().candidate().stallId()).distinct().count();
            assertThat(halls).isGreaterThanOrEqualTo(3);
            assertThat(result.stream().collect(Collectors.groupingBy(c -> c.scored().candidate().stallId(), Collectors.counting())).values())
                    .allMatch(n -> n <= 2);
            assertThat(result.stream().collect(Collectors.groupingBy(c -> c.scored().candidate().diningHallId(), Collectors.counting())).values())
                    .allMatch(n -> n <= 4);
            days.add(Map.of("date", date.toString(), "halls", halls, "stalls", stalls,
                    "dishIds", result.stream().map(c -> c.scored().candidate().id().toString()).toList()));
        }
        Map<Integer, Long> distribution = catalogue.stream().collect(Collectors.groupingBy(
                c -> history.getOrDefault(c.id(), List.of()).size(), TreeMap::new, Collectors.counting()));
        Map<String, Object> metrics = new TreeMap<>();
        metrics.put("algorithmVersion", config.algorithmVersion());
        metrics.put("configHash", config.configHash());
        metrics.put("catalogueSize", catalogue.size());
        metrics.put("days", 90);
        metrics.put("selected", 900);
        metrics.put("repeatWithin30Count", repeated);
        metrics.put("repeatWithin30Rate", repeated / 900d);
        metrics.put("uniqueDishes", history.size());
        metrics.put("coverage90Days", history.size() / 1200d);
        metrics.put("hallCoverageMin", days.stream().mapToLong(d -> (Long) d.get("halls")).min().orElseThrow());
        metrics.put("hallCoverageMean", days.stream().mapToLong(d -> (Long) d.get("halls")).average().orElseThrow());
        metrics.put("stallCoverageMin", days.stream().mapToLong(d -> (Long) d.get("stalls")).min().orElseThrow());
        metrics.put("stallCoverageMean", days.stream().mapToLong(d -> (Long) d.get("stalls")).average().orElseThrow());
        metrics.put("unratedRatio", unrated / 900d);
        metrics.put("qualityRatio", quality / 900d);
        metrics.put("exploreRatio", (900 - quality) / 900d);
        metrics.put("recommendationsPerDishDistribution", distribution);
        metrics.put("daily", days);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/recommendation-simulation.json"),
                JsonMapper.builder().build().writerWithDefaultPrettyPrinter().writeValueAsString(metrics));
        System.out.println("HybridRec simulation: " + metrics.entrySet().stream()
                .filter(e -> !e.getKey().equals("daily")).toList());
        assertThat(repeated).isZero();
        assertThat(history.size() / 1200d).isGreaterThanOrEqualTo(.60);
        // Scarce reviewed dishes cannot dominate: no dish can recur during its cooldown.
        assertThat(distribution.keySet()).allMatch(n -> n <= 3);
    }
}
