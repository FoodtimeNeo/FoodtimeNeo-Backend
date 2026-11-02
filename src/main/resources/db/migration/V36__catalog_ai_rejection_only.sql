-- Catalog uploads may receive AI advice/rejection, never AI approval.
ALTER TABLE foodtime.ai_moderation_results
    DROP CONSTRAINT ai_moderation_results_submission_type_check;
ALTER TABLE foodtime.ai_moderation_results
    ADD CONSTRAINT ai_moderation_results_submission_type_check CHECK (
        submission_type IN ('review','photo','dish','community-post','community-image',
            'community-comment','dish-review-reply')
    );

ALTER TABLE foodtime.dish_submissions ADD COLUMN reviewed_by_ai_model TEXT;
ALTER TABLE foodtime.dish_submissions DROP CONSTRAINT ck_dish_submissions_review_metadata;
ALTER TABLE foodtime.dish_submissions ADD CONSTRAINT ck_dish_submissions_review_metadata CHECK (
    status='pending' OR (reviewed_at IS NOT NULL AND num_nonnulls(reviewed_by_user_id,reviewed_by_ai_model)=1)
);
ALTER TABLE foodtime.dish_submissions ADD CONSTRAINT ck_dish_submissions_ai_rejection_only CHECK (
    reviewed_by_ai_model IS NULL OR (status='rejected' AND reviewed_by_user_id IS NULL)
);

-- Old approved photos may legitimately have no reviewer metadata (pre-V4).
-- Keep that history while forbidding any AI-attributed approval.
ALTER TABLE foodtime.dish_images ADD COLUMN reviewed_by_ai_model TEXT;
ALTER TABLE foodtime.dish_images ADD CONSTRAINT ck_dish_images_ai_rejection_only CHECK (
    reviewed_by_ai_model IS NULL OR
    (status='rejected' AND reviewed_by_user_id IS NULL AND reviewed_at IS NOT NULL)
);
COMMENT ON COLUMN foodtime.dish_submissions.reviewed_by_ai_model IS 'AI自动驳回的模型；通过只能由人工执行';
COMMENT ON COLUMN foodtime.dish_images.reviewed_by_ai_model IS 'AI自动驳回的模型；历史人工元数据保持原样';
