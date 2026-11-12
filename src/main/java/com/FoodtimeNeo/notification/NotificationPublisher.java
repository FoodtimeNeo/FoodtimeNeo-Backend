package com.FoodtimeNeo.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business modules call this in their own transaction. The event, inbox projection and
 * optional Android delivery row commit or roll back together; actual delivery is separate.
 */
@Service
public class NotificationPublisher {
    private final NotificationStore notifications;

    public NotificationPublisher(NotificationStore notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public void publish(NotificationCategory category, UUID recipient, UUID eventId, String title, String body,
            String refType, UUID refId, UUID refCommentId, UUID refRootCommentId) {
        notifications.ensurePreferences(recipient);
        long seq = notifications.insertEvent(eventId, recipient, category, title, body, refType, refId,
                refCommentId, refRootCommentId);
        notifications.insertInboxMessage(eventId, recipient, category, title, body, refType, refId,
                refCommentId, refRootCommentId);
        if (notifications.deliveryEnabled(recipient, category, seq)) {
            notifications.insertDelivery(eventId);
        }
    }
}
