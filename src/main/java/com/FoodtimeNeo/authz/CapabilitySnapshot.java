package com.FoodtimeNeo.authz;

import java.util.List;

/** Public, minimal capability projection for one authenticated actor. */
public record CapabilitySnapshot(List<String> capabilities, String revision) {
    public CapabilitySnapshot {
        capabilities = List.copyOf(capabilities);
    }
}
