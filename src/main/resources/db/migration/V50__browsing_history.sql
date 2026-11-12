-- 浏览历史：服务器时间与稳定顺序为权威；epoch 用于拒绝删除/清空前的离线任务回写。
CREATE SEQUENCE foodtime.browsing_history_seq AS BIGINT START WITH 1;

CREATE TABLE foodtime.browsing_history_settings (
    user_id UUID PRIMARY KEY REFERENCES foodtime.users(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT true,
    revision BIGINT NOT NULL DEFAULT 1,
    dish_epoch BIGINT NOT NULL DEFAULT 0,
    post_epoch BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_browsing_history_settings_revision CHECK (revision >= 1),
    CONSTRAINT ck_browsing_history_epochs CHECK (dish_epoch >= 0 AND post_epoch >= 0)
);

CREATE TABLE foodtime.browsing_history (
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    kind VARCHAR(8) NOT NULL,
    target_id UUID NOT NULL,
    visited_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    visit_seq BIGINT NOT NULL DEFAULT nextval('foodtime.browsing_history_seq'),
    epoch BIGINT NOT NULL,
    last_event_id UUID NOT NULL,
    PRIMARY KEY (user_id, kind, target_id),
    CONSTRAINT ck_browsing_history_kind CHECK (kind IN ('dish', 'post')),
    CONSTRAINT ck_browsing_history_epoch CHECK (epoch >= 0)
);
CREATE INDEX ix_browsing_history_recent
    ON foodtime.browsing_history (user_id, kind, visited_at DESC, visit_seq DESC);

CREATE TRIGGER trg_browsing_history_settings_updated_at
    BEFORE UPDATE ON foodtime.browsing_history_settings
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
