package com.FoodtimeNeo.notification;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record Preference(String category, boolean enabled, long revision) {}

    public record Preferences(List<Preference> items) {
        public Preferences {
            items = List.copyOf(items);
        }
    }

    public record PreferenceRequest(@NotNull Boolean enabled, @NotNull Long expectedRevision) {}

    public record LegacyMessagePreferenceRequest(@NotNull Boolean enabled) {}

    public record Event(UUID eventId, long seq, String category, String title, String body,
                        String refType, UUID refId, UUID refCommentId, UUID refRootCommentId,
                        Instant createdAt, String deliveryStatus) {
        public Event withDeliveryStatus(String status) {
            return new Event(eventId, seq, category, title, body, refType, refId, refCommentId,
                    refRootCommentId, createdAt, status);
        }
    }

    public record EventPage(List<Event> items, long nextSeq) {
        public EventPage {
            items = List.copyOf(items);
        }
    }
}
