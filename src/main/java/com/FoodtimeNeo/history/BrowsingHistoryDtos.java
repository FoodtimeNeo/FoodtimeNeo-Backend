package com.FoodtimeNeo.history;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class BrowsingHistoryDtos {
    private BrowsingHistoryDtos() {}

    public record SettingsResponse(boolean enabled, long revision, long dishEpoch, long postEpoch) {}

    public record SettingsRequest(@NotNull Boolean enabled, @NotNull Long expectedRevision) {}

    public record RecordRequest(@NotNull Long epoch, @NotNull UUID visitEventId) {}

    public record Item(String kind, UUID targetId, UUID diningHallId, UUID stallId, Instant visitedAt,
                       boolean available, String title, String subtitle, String imageUrl) {}
}
