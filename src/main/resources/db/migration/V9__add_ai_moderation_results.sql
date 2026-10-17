-- AI triage results for pending submissions. One row per submission at most; triage
-- never modifies submissions or creates formal records. Use schema-qualified names
-- so this migration is independent of search_path.

CREATE TABLE foodtime.ai_moderation_results (
    id UUID PRIMARY KEY,
    submission_type VARCHAR(20) NOT NULL CHECK (submission_type IN ('review', 'photo')),
    submission_id UUID NOT NULL,
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    confidence NUMERIC(4, 3) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    labels TEXT[] NOT NULL DEFAULT '{}',
    reason TEXT,
    model VARCHAR(100) NOT NULL,
    checked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ai_moderation_result UNIQUE (submission_type, submission_id)
);
CREATE INDEX ix_ai_moderation_results_submission ON foodtime.ai_moderation_results (submission_type, submission_id);
COMMENT ON TABLE foodtime.ai_moderation_results IS 'AI预检结论：只做分诊，不修改投稿，不生成正式记录';
COMMENT ON COLUMN foodtime.ai_moderation_results.labels IS 'AI打标数组，如 广告/色情/模糊；原文透传';
