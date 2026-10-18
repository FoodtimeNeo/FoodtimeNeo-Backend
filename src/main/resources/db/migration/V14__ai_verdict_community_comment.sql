-- P1 遗漏了社区评论类型：结论类型放开 community-comment。
ALTER TABLE foodtime.ai_moderation_results
    DROP CONSTRAINT ai_moderation_results_submission_type_check;
ALTER TABLE foodtime.ai_moderation_results
    ADD CONSTRAINT ai_moderation_results_submission_type_check CHECK (
        submission_type IN ('review', 'photo', 'community-post', 'community-image', 'community-comment'));
