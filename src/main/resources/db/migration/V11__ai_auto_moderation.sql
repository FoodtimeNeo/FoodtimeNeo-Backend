-- AI 自动审核：结论够硬的投稿由模型直接拍板，人只处理拿不准的。
-- reviewed_by_ai_model 与人工审核人互斥；日志用 actor 区分 AI 与人工，方便核验时分开看。
ALTER TABLE foodtime.dish_review_submissions
    ADD COLUMN reviewed_by_ai_model TEXT;

ALTER TABLE foodtime.dish_review_submissions
    DROP CONSTRAINT ck_review_submissions_review_metadata;
ALTER TABLE foodtime.dish_review_submissions
    ADD CONSTRAINT ck_review_submissions_review_metadata CHECK (
        status = 'pending' OR (reviewed_at IS NOT NULL
            AND (reviewed_by_user_id IS NOT NULL OR reviewed_by_ai_model IS NOT NULL)));

ALTER TABLE foodtime.content_moderation_logs
    ADD COLUMN actor VARCHAR(20) NOT NULL DEFAULT 'human',
    ADD COLUMN ai_model TEXT;
ALTER TABLE foodtime.content_moderation_logs
    ALTER COLUMN moderator_user_id DROP NOT NULL;
ALTER TABLE foodtime.content_moderation_logs
    ADD CONSTRAINT ck_moderation_logs_actor CHECK (actor IN ('human', 'ai')),
    ADD CONSTRAINT ck_moderation_logs_actor_identity CHECK (
        (actor = 'human') = (moderator_user_id IS NOT NULL)),
    ADD CONSTRAINT ck_moderation_logs_ai_identity CHECK (
        (actor = 'ai') = (ai_model IS NOT NULL));

COMMENT ON COLUMN foodtime.dish_review_submissions.reviewed_by_ai_model IS 'AI自动审核的模型名；人工审核时为空';
COMMENT ON COLUMN foodtime.content_moderation_logs.actor IS '审核动作来源：human人工，ai模型自动；核验时分开看';
COMMENT ON COLUMN foodtime.content_moderation_logs.ai_model IS 'AI自动审核的模型名；人工审核时为空';
