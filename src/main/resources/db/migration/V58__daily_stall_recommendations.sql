CREATE TABLE foodtime.daily_recommendation_stall_item (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES foodtime.daily_recommendation_batch(id) ON DELETE CASCADE,
    stall_id UUID NOT NULL REFERENCES foodtime.stalls(id) ON DELETE RESTRICT,
    position INTEGER NOT NULL,
    recommendation_type VARCHAR(20) NOT NULL,
    recommendation_score NUMERIC(12,9) NOT NULL,
    reason_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_recommendation_stall UNIQUE (batch_id, stall_id),
    CONSTRAINT uq_daily_recommendation_stall_position UNIQUE (batch_id, position),
    CONSTRAINT ck_daily_recommendation_stall_position CHECK (position > 0),
    CONSTRAINT ck_daily_recommendation_stall_type CHECK (recommendation_type IN ('QUALITY', 'EXPLORE')),
    CONSTRAINT ck_daily_recommendation_stall_score CHECK (recommendation_score >= 0 AND recommendation_score <= 1),
    CONSTRAINT ck_daily_recommendation_stall_reason CHECK (reason_code IN
        ('GOOD_REPUTATION', 'RECENTLY_POPULAR', 'DAILY_DISCOVERY', 'LESS_EXPOSED'))
);

CREATE INDEX ix_daily_recommendation_stall_history
    ON foodtime.daily_recommendation_stall_item (stall_id, batch_id);
