-- FoodtimeNeo Daily HybridRec V2: one idempotent, auditable snapshot per business day.
CREATE TABLE foodtime.daily_recommendation_batch (
    id UUID PRIMARY KEY,
    recommendation_date DATE NOT NULL UNIQUE,
    algorithm_version VARCHAR(64) NOT NULL,
    config_hash VARCHAR(64) NOT NULL,
    generation_status VARCHAR(20) NOT NULL DEFAULT 'generating',
    generated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_daily_recommendation_batch_status
        CHECK (generation_status IN ('generating', 'success', 'failed')),
    CONSTRAINT ck_daily_recommendation_batch_generated_at
        CHECK (generation_status <> 'success' OR generated_at IS NOT NULL)
);

CREATE TABLE foodtime.daily_recommendation_item (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES foodtime.daily_recommendation_batch(id) ON DELETE CASCADE,
    dish_id UUID NOT NULL REFERENCES foodtime.dishes(id) ON DELETE RESTRICT,
    position INTEGER NOT NULL,
    recommendation_type VARCHAR(20) NOT NULL,
    recommendation_score NUMERIC(12,9) NOT NULL,
    reason_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_daily_recommendation_item_dish UNIQUE (batch_id, dish_id),
    CONSTRAINT uq_daily_recommendation_item_position UNIQUE (batch_id, position),
    CONSTRAINT ck_daily_recommendation_item_position CHECK (position > 0),
    CONSTRAINT ck_daily_recommendation_item_type CHECK (recommendation_type IN ('QUALITY', 'EXPLORE')),
    CONSTRAINT ck_daily_recommendation_item_score CHECK (recommendation_score >= 0 AND recommendation_score <= 1),
    CONSTRAINT ck_daily_recommendation_item_reason CHECK (reason_code IN
        ('GOOD_REPUTATION', 'RECENTLY_POPULAR', 'DAILY_DISCOVERY', 'LESS_EXPOSED'))
);
CREATE INDEX ix_daily_recommendation_item_dish_history
    ON foodtime.daily_recommendation_item (dish_id, batch_id);

CREATE TRIGGER trg_daily_recommendation_batch_updated_at
    BEFORE UPDATE ON foodtime.daily_recommendation_batch
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
