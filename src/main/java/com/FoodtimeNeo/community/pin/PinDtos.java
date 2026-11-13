package com.FoodtimeNeo.community.pin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class PinDtos {
    public record Entry(UUID id, String title, String userDisplayName, String status, int position) { }
    public record Snapshot(long revision, List<Entry> items, boolean canManage) {
        public Snapshot { items=List.copyOf(items); }
    }
    public record Change(@NotNull @Min(0) Long expectedRevision) { }
    public record Order(@NotNull @Min(0) Long expectedRevision,
                        @NotNull @Size(max=3) List<@NotNull UUID> postIds) { }
    private PinDtos() { }
}
