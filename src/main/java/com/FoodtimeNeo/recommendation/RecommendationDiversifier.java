package com.FoodtimeNeo.recommendation;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

import static com.FoodtimeNeo.recommendation.RecommendationModels.*;

@Component
public class RecommendationDiversifier {
    private static final Comparator<ScoredCandidate> SCORE_ORDER =
            Comparator.comparingDouble(ScoredCandidate::score).reversed()
                    .thenComparing(Comparator.comparingDouble(ScoredCandidate::rotation).reversed())
                    .thenComparing(c -> c.candidate().id());
    private static final Predicate<ScoredCandidate> QUALITY = c -> c.candidate().reviewCount() >= 1;
    private static final Predicate<ScoredCandidate> EXPLORE =
            c -> c.candidate().reviewCount() < 5 || c.candidate().exposure90d() <= 1;

    public List<SelectedCandidate> select(List<ScoredCandidate> scored, int qualityQuota,
                                         int targetSize, int maxPerStall, int maxPerHall) {
        return select(scored, qualityQuota, targetSize, maxPerStall, maxPerHall, 3, LocalDate.MIN, 0);
    }

    public List<SelectedCandidate> select(List<ScoredCandidate> scored, int qualityQuota, int targetSize,
            int maxPerStall, int maxPerHall, int minimumHalls, LocalDate date, int cooldownDays) {
        LocalDate boundary = cooldownDays == 0 ? date : date.minusDays(cooldownDays);
        List<ScoredCandidate> available = scored.stream().filter(c -> c.candidate().lastRecommendedDate() == null
                || c.candidate().lastRecommendedDate().isBefore(boundary)).toList();
        List<ScoredCandidate> cooling = scored.stream().filter(c -> c.candidate().lastRecommendedDate() != null
                && !c.candidate().lastRecommendedDate().isBefore(boundary)).toList();
        Selection selection = new Selection(targetSize, minimumHalls);
        selection.fill(available.stream().filter(QUALITY).toList(), qualityQuota,
                "QUALITY", maxPerStall, maxPerHall, false);
        selection.fill(available.stream().filter(EXPLORE).toList(), targetSize - selection.items.size(),
                "EXPLORE", maxPerStall, maxPerHall, false);
        // Quotas relax before diversity; all remaining non-cooling candidates are considered.
        selection.fill(available, targetSize, null, maxPerStall, maxPerHall, false);
        selection.relax(available, maxPerStall, maxPerHall, false);
        // Shared counts/IDs survive every stage. Cooling candidates are ordered by oldest last date.
        selection.fill(cooling, targetSize, null, maxPerStall, maxPerHall, true);
        selection.relax(cooling, maxPerStall, maxPerHall, true);
        return List.copyOf(selection.items);
    }

    private static class Selection {
        final int target;
        final int minimumHalls;
        final List<SelectedCandidate> items = new ArrayList<>();
        final Set<UUID> ids = new HashSet<>();
        final Map<UUID, Integer> halls = new HashMap<>();
        final Map<UUID, Integer> stalls = new HashMap<>();

        Selection(int target, int minimumHalls) {
            this.target = target;
            this.minimumHalls = Math.min(minimumHalls, target);
        }

        void relax(List<ScoredCandidate> pool, int stallCap, int hallCap, boolean oldestFirst) {
            while (items.size() < target && (stallCap < target || hallCap < target)) {
                stallCap = Math.min(target, stallCap + 1);
                hallCap = Math.min(target, hallCap + 1);
                fill(pool, target, null, stallCap, hallCap, oldestFirst);
            }
        }

        void fill(List<ScoredCandidate> pool, int limit, String type,
                  int stallCap, int hallCap, boolean oldestFirst) {
            for (int added = 0; added < limit && items.size() < target; added++) {
                // Re-evaluate coverage after each selection; never use a stale one-time sort.
                Comparator<ScoredCandidate> order = Comparator
                        .comparingInt((ScoredCandidate c) -> halls.size() < minimumHalls
                                && !halls.containsKey(c.candidate().diningHallId()) ? 0 : 1)
                        .thenComparingInt(c -> !stalls.containsKey(c.candidate().stallId()) ? 0 : 1)
                        .thenComparing(SCORE_ORDER);
                if (oldestFirst) order = Comparator
                        .comparing((ScoredCandidate c) -> c.candidate().lastRecommendedDate()).thenComparing(order);
                ScoredCandidate next = pool.stream().filter(c -> !ids.contains(c.candidate().id()))
                        .filter(c -> halls.getOrDefault(c.candidate().diningHallId(), 0) < hallCap)
                        .filter(c -> stalls.getOrDefault(c.candidate().stallId(), 0) < stallCap)
                        .min(order).orElse(null);
                if (next == null) return;
                String actualType = type != null ? type : EXPLORE.test(next) ? "EXPLORE" : "QUALITY";
                items.add(new SelectedCandidate(next, actualType, reason(next)));
                ids.add(next.candidate().id());
                halls.merge(next.candidate().diningHallId(), 1, Integer::sum);
                stalls.merge(next.candidate().stallId(), 1, Integer::sum);
            }
        }
    }

    private static String reason(ScoredCandidate candidate) {
        if (candidate.candidate().reviewCount() >= 5 && candidate.quality() >= .75) return "GOOD_REPUTATION";
        if (candidate.candidate().reviews7d() > 0 && candidate.heat() >= .5) return "RECENTLY_POPULAR";
        if (candidate.candidate().exposure90d() == 0) return "LESS_EXPOSED";
        return "DAILY_DISCOVERY";
    }
}
