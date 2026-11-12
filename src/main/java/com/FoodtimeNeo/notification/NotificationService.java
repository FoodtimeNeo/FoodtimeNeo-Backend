package com.FoodtimeNeo.notification;

import com.FoodtimeNeo.common.exception.BusinessException;
import com.FoodtimeNeo.notice.NoticeEventService;
import com.FoodtimeNeo.notification.NotificationDtos.Event;
import com.FoodtimeNeo.notification.NotificationDtos.EventPage;
import com.FoodtimeNeo.notification.NotificationDtos.Preference;
import com.FoodtimeNeo.notification.NotificationDtos.Preferences;
import com.FoodtimeNeo.notification.NotificationDtos.PreferenceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private static final int MAX_EVENTS = 100;
    private final NotificationStore notifications;
    private final NoticeEventService notices;

    public NotificationService(NotificationStore notifications, NoticeEventService notices) {
        this.notifications = notifications;
        this.notices = notices;
    }

    @Transactional
    public Preferences preferences(UUID user) {
        return new Preferences(notifications.preferences(user));
    }

    @Transactional
    public Preference update(UUID user, String categoryValue, PreferenceRequest request) {
        NotificationCategory category = category(categoryValue);
        notifications.lockPreference(user, category);
        if (category == NotificationCategory.NOTICE && request.enabled()) {
            // Existing visible notices become part of the enable baseline, never replay.
            notices.ensureVisible(user);
        }
        if (!notifications.updatePreference(user, category, request.enabled(), request.expectedRevision())) {
            throw new BusinessException("NOTIFICATION_SETTINGS_CONFLICT", "提醒设置已被其他操作修改，请刷新后重试",
                    HttpStatus.CONFLICT);
        }
        return notifications.preference(user, category);
    }

    @Transactional
    public EventPage events(UUID user, long afterSeq, int limit) {
        if (afterSeq < 0) throw new BusinessException("VALIDATION_ERROR", "afterSeq不能为负数",
                HttpStatus.BAD_REQUEST);
        notices.ensureVisible(user);
        int size = Math.min(Math.max(limit, 1), MAX_EVENTS);
        List<Event> items = notifications.events(user, afterSeq, size);
        long next = items.isEmpty() ? afterSeq : items.get(items.size() - 1).seq();
        return new EventPage(items, next);
    }

    @Transactional
    public void delivered(UUID user, UUID eventId) {
        if (!notifications.markDelivered(eventId, user)) {
            throw new BusinessException("NOTIFICATION_NOT_DELIVERABLE", "该事件不属于当前用户或没有投递记录",
                    HttpStatus.CONFLICT);
        }
    }

    private static NotificationCategory category(String value) {
        return NotificationCategory.fromWire(value).orElseThrow(() ->
                new BusinessException("VALIDATION_ERROR", "category不合法", HttpStatus.BAD_REQUEST));
    }
}
