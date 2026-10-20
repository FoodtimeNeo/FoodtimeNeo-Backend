-- 社区动态纳入AI分诊：结论类型放开到 community-post（正文）与 community-image（配图）。
ALTER TABLE foodtime.ai_moderation_results
    DROP CONSTRAINT ai_moderation_results_submission_type_check;
ALTER TABLE foodtime.ai_moderation_results
    ADD CONSTRAINT ai_moderation_results_submission_type_check CHECK (
        submission_type IN ('review', 'photo', 'community-post', 'community-image'));
