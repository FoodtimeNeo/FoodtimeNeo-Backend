-- 菜品点评互动 V2：高频读取方案，基于当前 V14 数据库结构。
-- 已由 dish-review-interactions-v3.sql 替代；保留历史，不与 V3 叠加执行。
-- 这是设计草案，不位于 Flyway migration 目录，也未在实际数据库执行。
-- 必须配套调整后端软删除、审核发布 UPSERT、可见性和审核类型后再部署。
-- 仅支持一级点评点赞；回复不评分。所有 UUID 由服务端生成。

BEGIN;

-- 1. 点评保留正文与生命周期；计数放入独立的小表。
ALTER TABLE foodtime.dish_reviews ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE foodtime.dish_reviews DROP CONSTRAINT uq_dish_reviews_dish_user;

CREATE UNIQUE INDEX ux_dish_reviews_active_dish_user
    ON foodtime.dish_reviews (dish_id, user_id)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_dish_reviews_active_page
    ON foodtime.dish_reviews (dish_id, created_at DESC, id ASC)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_dish_reviews_active_owner_page
    ON foodtime.dish_reviews (user_id, created_at DESC, id ASC)
    WHERE deleted_at IS NULL;

-- 2. 一条点评对应一条统计；热更新字段不建二级索引。
-- fillfactor 为初始建议值，需根据实际压测调整；不保证每次更新都能使用 HOT。
CREATE TABLE foodtime.dish_review_stats (
    review_id UUID PRIMARY KEY REFERENCES foodtime.dish_reviews(id) ON DELETE RESTRICT,
    like_count BIGINT NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    reply_count BIGINT NOT NULL DEFAULT 0 CHECK (reply_count >= 0),
    counter_version BIGINT NOT NULL DEFAULT 0 CHECK (counter_version >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
) WITH (fillfactor = 80);

-- V14 尚无菜品点评点赞/回复明细，所以现有点评的初始计数均为零。
INSERT INTO foodtime.dish_review_stats(review_id)
SELECT id FROM foodtime.dish_reviews;

CREATE FUNCTION foodtime.initialize_dish_review_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO foodtime.dish_review_stats(review_id) VALUES (NEW.id);
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_reviews_initialize_stats
    AFTER INSERT ON foodtime.dish_reviews
    FOR EACH ROW EXECUTE FUNCTION foodtime.initialize_dish_review_stats();

-- 3. 点赞明细：复合主键同时承担去重和按点评读取本人点赞状态的索引。
CREATE TABLE foodtime.dish_review_likes (
    review_id UUID NOT NULL REFERENCES foodtime.dish_reviews(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (review_id, user_id)
);

-- 4. 楼内回复：直接回复点评时 reply_to_id 为空；回复其他回复时填写目标 ID。
CREATE TABLE foodtime.dish_review_replies (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES foodtime.dish_reviews(id) ON DELETE RESTRICT,
    reply_to_id UUID,
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewed_by_user_id UUID REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    reviewed_by_ai_model TEXT,
    reviewed_at TIMESTAMPTZ,
    review_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_dish_review_replies_scope UNIQUE (review_id, id),
    CONSTRAINT fk_dish_review_replies_same_review
        FOREIGN KEY (review_id, reply_to_id)
        REFERENCES foodtime.dish_review_replies(review_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_dish_review_replies_not_self CHECK (reply_to_id IS NULL OR reply_to_id <> id),
    CONSTRAINT ck_dish_review_replies_content CHECK (char_length(btrim(content)) BETWEEN 1 AND 500),
    CONSTRAINT ck_dish_review_replies_status CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT ck_dish_review_replies_review_metadata CHECK (
        (status = 'pending' AND reviewed_at IS NULL
            AND num_nonnulls(reviewed_by_user_id, reviewed_by_ai_model) = 0)
        OR (status IN ('approved', 'rejected') AND reviewed_at IS NOT NULL
            AND num_nonnulls(reviewed_by_user_id, reviewed_by_ai_model) = 1)
    )
);

CREATE INDEX ix_dish_review_replies_public_page
    ON foodtime.dish_review_replies (review_id, created_at ASC, id ASC)
    WHERE status = 'approved' AND deleted_at IS NULL;
CREATE INDEX ix_dish_review_replies_pending_page
    ON foodtime.dish_review_replies (created_at ASC, id ASC)
    WHERE status = 'pending' AND deleted_at IS NULL;
CREATE INDEX ix_dish_review_replies_owner_page
    ON foodtime.dish_review_replies (user_id, created_at DESC, id ASC)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_dish_review_replies_target
    ON foodtime.dish_review_replies (review_id, reply_to_id)
    WHERE reply_to_id IS NOT NULL;

-- 提交后不允许改变作者、正文、归属或回复对象；避免跨楼迁移和回复关系环。
-- 最终审核结果不可重审；已删除的回复不可恢复或继续审核。
CREATE FUNCTION foodtime.guard_dish_review_reply_update()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.review_id IS DISTINCT FROM OLD.review_id
        OR NEW.reply_to_id IS DISTINCT FROM OLD.reply_to_id
        OR NEW.user_id IS DISTINCT FROM OLD.user_id
        OR NEW.content IS DISTINCT FROM OLD.content
        OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'Reply identity, content and target are immutable' USING ERRCODE = '23514';
    END IF;
    IF OLD.status <> 'pending' AND NEW.status IS DISTINCT FROM OLD.status THEN
        RAISE EXCEPTION 'Final reply verdict is immutable' USING ERRCODE = '23514';
    END IF;
    IF OLD.deleted_at IS NOT NULL AND (
        NEW.deleted_at IS DISTINCT FROM OLD.deleted_at OR NEW.status IS DISTINCT FROM OLD.status
    ) THEN
        RAISE EXCEPTION 'Deleted reply cannot be restored or moderated' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_review_replies_guard
    BEFORE UPDATE ON foodtime.dish_review_replies
    FOR EACH ROW EXECUTE FUNCTION foodtime.guard_dish_review_reply_update();
CREATE TRIGGER trg_dish_review_replies_updated_at
    BEFORE UPDATE ON foodtime.dish_review_replies
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();

-- 5. 只有明细实际插入/删除时才改变点赞数；ON CONFLICT DO NOTHING 不会加赞。
-- 计数变化与原操作在同一事务内；异常会回滚整个操作。
CREATE FUNCTION foodtime.adjust_dish_review_like_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    changed_review_id UUID;
    delta BIGINT;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Like rows are immutable; use INSERT or DELETE' USING ERRCODE = '23514';
    ELSIF TG_OP = 'INSERT' THEN
        changed_review_id := NEW.review_id;
        delta := 1;
    ELSE
        changed_review_id := OLD.review_id;
        delta := -1;
    END IF;
    UPDATE foodtime.dish_review_stats
    SET like_count = like_count + delta,
        counter_version = counter_version + 1,
        updated_at = statement_timestamp()
    WHERE review_id = changed_review_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Missing stats row for review %', changed_review_id USING ERRCODE = '23503';
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_review_likes_stats
    AFTER INSERT OR UPDATE OR DELETE ON foodtime.dish_review_likes
    FOR EACH ROW EXECUTE FUNCTION foodtime.adjust_dish_review_like_stats();

-- 只有 approved 且未删除的回复计数；pending 的新增/删除、驳回均不写统计行。
CREATE FUNCTION foodtime.adjust_dish_review_reply_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    changed_review_id UUID;
    old_visible BIGINT := 0;
    new_visible BIGINT := 0;
    delta BIGINT;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        changed_review_id := OLD.review_id;
        IF OLD.status = 'approved' AND OLD.deleted_at IS NULL THEN old_visible := 1; END IF;
    END IF;
    IF TG_OP <> 'DELETE' THEN
        changed_review_id := NEW.review_id;
        IF NEW.status = 'approved' AND NEW.deleted_at IS NULL THEN new_visible := 1; END IF;
    END IF;
    delta := new_visible - old_visible;
    IF delta <> 0 THEN
        UPDATE foodtime.dish_review_stats
        SET reply_count = reply_count + delta,
            counter_version = counter_version + 1,
            updated_at = statement_timestamp()
        WHERE review_id = changed_review_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Missing stats row for review %', changed_review_id USING ERRCODE = '23503';
        END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_review_replies_stats
    AFTER INSERT OR UPDATE OR DELETE ON foodtime.dish_review_replies
    FOR EACH ROW EXECUTE FUNCTION foodtime.adjust_dish_review_reply_stats();

-- 6. 统一审核日志及 AI 结论增加回复类型；复用已有 human/ai 身份约束。
ALTER TABLE foodtime.content_moderation_logs
    ADD COLUMN dish_review_reply_id UUID
        REFERENCES foodtime.dish_review_replies(id) ON DELETE RESTRICT;
ALTER TABLE foodtime.content_moderation_logs DROP CONSTRAINT ck_moderation_logs_one_submission;
ALTER TABLE foodtime.content_moderation_logs
    ADD CONSTRAINT ck_moderation_logs_one_submission CHECK (
        num_nonnulls(review_submission_id, dish_submission_id, stall_submission_id,
            dish_image_id, community_post_id, community_comment_id, dish_review_reply_id) = 1
    );
CREATE INDEX ix_moderation_logs_dish_review_reply
    ON foodtime.content_moderation_logs (dish_review_reply_id)
    WHERE dish_review_reply_id IS NOT NULL;

ALTER TABLE foodtime.ai_moderation_results
    DROP CONSTRAINT ai_moderation_results_submission_type_check;
ALTER TABLE foodtime.ai_moderation_results
    ADD CONSTRAINT ai_moderation_results_submission_type_check CHECK (
        submission_type IN ('review', 'photo', 'community-post', 'community-image',
            'community-comment', 'dish-review-reply')
    );

-- 7. 软删除点评不再参与评分；回复始终不参与评分。
-- 在真实发布时评估重建时间和锁等待，与应用变更协调执行。
DROP MATERIALIZED VIEW foodtime.dish_rating_summary;
CREATE MATERIALIZED VIEW foodtime.dish_rating_summary AS
SELECT d.id AS dish_id,
       AVG(r.rating)::NUMERIC(3,2) AS average_score,
       COUNT(r.id) AS review_count
FROM foodtime.dishes d
LEFT JOIN foodtime.dish_reviews r ON r.dish_id = d.id AND r.deleted_at IS NULL
GROUP BY d.id
WITH DATA;
CREATE UNIQUE INDEX ux_dish_rating_summary_dish_id ON foodtime.dish_rating_summary (dish_id);

COMMENT ON TABLE foodtime.dish_review_stats IS '点评互动统计：事务内维护；读取路径不统计明细';
COMMENT ON COLUMN foodtime.dish_review_stats.reply_count IS '本点评下approved且未删除的回复数；主点评删除仅隐藏整楼，不清零历史计数';
COMMENT ON COLUMN foodtime.dish_review_stats.counter_version IS '计数每次有效变更递增；客户端据此忽略较旧的计数响应';
COMMENT ON TABLE foodtime.dish_review_likes IS '菜品一级点评点赞明细；主键去重，取消点赞删除明细';
COMMENT ON TABLE foodtime.dish_review_replies IS '菜品点评楼内回复；自带审核状态、软删除；正文和回复关系不可修改';
COMMENT ON COLUMN foodtime.dish_reviews.deleted_at IS '软删除时间；已删除点评及整楼互动对公开接口不可见';
COMMENT ON MATERIALIZED VIEW foodtime.dish_rating_summary IS '未删除正式点评的评分汇总；由应用每24小时并发刷新';

COMMIT;
