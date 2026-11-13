-- 通知事件/outbox 基础：稳定 eventId、站内消息投影、Android 投递记录与七类偏好。
CREATE SEQUENCE foodtime.notification_event_seq AS BIGINT START WITH 1;

CREATE TABLE foodtime.notification_events (
    id UUID PRIMARY KEY,
    seq BIGINT NOT NULL DEFAULT nextval('foodtime.notification_event_seq'),
    recipient_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    category VARCHAR(16) NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    ref_type TEXT,
    ref_id UUID,
    ref_comment_id UUID,
    ref_root_comment_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_notification_event_seq UNIQUE (seq),
    CONSTRAINT ck_notification_events_category
        CHECK (category IN ('message', 'interaction', 'notice', 'feedback', 'correction', 'report', 'appeal'))
);
CREATE INDEX ix_notification_events_recipient_seq
    ON foodtime.notification_events (recipient_id, seq);

CREATE TABLE foodtime.notification_preferences (
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    category VARCHAR(16) NOT NULL,
    enabled BOOLEAN NOT NULL,
    revision BIGINT NOT NULL DEFAULT 1,
    enabled_after_seq BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, category),
    CONSTRAINT ck_notification_preferences_category
        CHECK (category IN ('message', 'interaction', 'notice', 'feedback', 'correction', 'report', 'appeal')),
    CONSTRAINT ck_notification_preferences_revision CHECK (revision >= 1),
    CONSTRAINT ck_notification_preferences_baseline CHECK (enabled_after_seq >= 0)
);

CREATE TABLE foodtime.notification_deliveries (
    event_id UUID NOT NULL REFERENCES foodtime.notification_events(id) ON DELETE CASCADE,
    channel VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, channel),
    CONSTRAINT ck_notification_deliveries_channel CHECK (channel IN ('android')),
    CONSTRAINT ck_notification_deliveries_status CHECK (status IN ('pending', 'delivered', 'failed', 'skipped')),
    CONSTRAINT ck_notification_deliveries_attempts CHECK (attempts >= 0)
);
CREATE INDEX ix_notification_deliveries_pending
    ON foodtime.notification_deliveries (status, next_attempt_at)
    WHERE status IN ('pending', 'failed');

ALTER TABLE foodtime.messages ADD COLUMN event_id UUID;
CREATE UNIQUE INDEX uq_messages_event
    ON foodtime.messages (event_id) WHERE event_id IS NOT NULL;

CREATE TRIGGER trg_notification_preferences_updated_at
    BEFORE UPDATE ON foodtime.notification_preferences
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
CREATE TRIGGER trg_notification_deliveries_updated_at
    BEFORE UPDATE ON foodtime.notification_deliveries
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
