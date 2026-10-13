-- Approved by the project owner: independent image review needs reason/time/actor and audit linkage.
-- Existing approved/rejected images keep NULL metadata; no fabricated historical moderator or time.
ALTER TABLE foodtime.dish_images
    ADD COLUMN reviewed_by_user_id UUID REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN review_reason TEXT;
ALTER TABLE foodtime.content_moderation_logs
    ADD COLUMN dish_image_id UUID REFERENCES foodtime.dish_images(id) ON DELETE RESTRICT;
ALTER TABLE foodtime.content_moderation_logs DROP CONSTRAINT ck_moderation_logs_one_submission;
ALTER TABLE foodtime.content_moderation_logs ADD CONSTRAINT ck_moderation_logs_one_submission
    CHECK (num_nonnulls(review_submission_id,dish_submission_id,stall_submission_id,dish_image_id) = 1);
CREATE INDEX ix_moderation_logs_dish_image ON foodtime.content_moderation_logs(dish_image_id)
    WHERE dish_image_id IS NOT NULL;
COMMENT ON COLUMN foodtime.dish_images.review_reason IS '图片审核说明；历史记录可为空';
COMMENT ON COLUMN foodtime.content_moderation_logs.dish_image_id IS '独立图片审核的目标；四类目标必须且只能填写一个';
