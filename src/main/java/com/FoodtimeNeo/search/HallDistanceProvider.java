package com.FoodtimeNeo.search;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Replace config-based coordinates without changing catalog or note search contracts. */
public interface HallDistanceProvider {
    Map<UUID, Double> distances(Double latitude, Double longitude);
    Set<UUID> hallIds();
}
