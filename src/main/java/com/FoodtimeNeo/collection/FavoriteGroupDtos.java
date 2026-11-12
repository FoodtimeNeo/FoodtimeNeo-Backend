package com.FoodtimeNeo.collection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class FavoriteGroupDtos {
    private FavoriteGroupDtos() {}

    public record Response(UUID id, String kind, String name, int sortOrder, long revision, long itemCount) {}

    public record CreateRequest(@NotBlank String kind, @NotBlank @Size(max = 80) String name, Integer sortOrder) {}

    public record UpdateRequest(@Size(max = 80) String name, Integer sortOrder, Long expectedRevision) {}

    public record MembershipResponse(List<UUID> groupIds) {
        public MembershipResponse {
            groupIds = List.copyOf(groupIds);
        }
    }
}
