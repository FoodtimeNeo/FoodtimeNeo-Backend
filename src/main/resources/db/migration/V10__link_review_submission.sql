-- 已通过的评论保留其投稿ID，管理端才能连出当时的AI分诊结论；
-- 老数据与外部灌入的行留空，前端显示为暂无AI结论。
ALTER TABLE foodtime.dish_reviews
    ADD COLUMN submission_id UUID REFERENCES foodtime.dish_review_submissions (id) ON DELETE SET NULL;

COMMENT ON COLUMN foodtime.dish_reviews.submission_id IS '来源投稿ID：审核通过时写入，用于关联AI预检结论；置空表示无结论';
