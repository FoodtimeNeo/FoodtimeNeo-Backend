-- 菜品点评与实时评分 V3，供审查，基于当前 V14；替代 V2 草案，不与 V2 叠加执行。
-- 尚未执行；不是 Flyway 自动迁移。不得在旧应用仍写入时单独部署。
-- 配套改动：取消评分定时任务、修改审核 UPSERT/软删除/可见性、接入回复及两类点赞。
-- 所有 UUID 由服务端/客户端操作 ID 生成；这里只定义持久化规则。

BEGIN;

-- 回填和触发器安装必须处于同一个停写发布窗口，防止遗漏并发写入。
LOCK TABLE foodtime.dishes, foodtime.dish_reviews IN SHARE ROW EXCLUSIVE MODE;

ALTER TABLE foodtime.dish_reviews ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE foodtime.dish_reviews DROP CONSTRAINT uq_dish_reviews_dish_user;
CREATE UNIQUE INDEX ux_dish_reviews_active_dish_user
    ON foodtime.dish_reviews(dish_id, user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_dish_reviews_active_page
    ON foodtime.dish_reviews(dish_id, created_at DESC, id ASC) WHERE deleted_at IS NULL;
CREATE INDEX ix_dish_reviews_active_owner_page
    ON foodtime.dish_reviews(user_id, created_at DESC, id ASC) WHERE deleted_at IS NULL;

-- 1. 菜品级实时统计。平均分通过后面的普通视图计算，不单独维护浮点均值。
CREATE TABLE foodtime.dish_stats (
    dish_id UUID PRIMARY KEY REFERENCES foodtime.dishes(id) ON DELETE CASCADE,
    review_count BIGINT NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    rating_sum BIGINT NOT NULL DEFAULT 0 CHECK (rating_sum >= 0),
    counter_version BIGINT NOT NULL DEFAULT 0 CHECK (counter_version >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_dish_stats_rating_sum CHECK (
        rating_sum::NUMERIC BETWEEN review_count::NUMERIC AND review_count::NUMERIC * 5
    )
) WITH (fillfactor = 80);

-- 2. 一级点评的点赞数、可见回复数。
CREATE TABLE foodtime.dish_review_stats (
    review_id UUID PRIMARY KEY REFERENCES foodtime.dish_reviews(id) ON DELETE CASCADE,
    like_count BIGINT NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    reply_count BIGINT NOT NULL DEFAULT 0 CHECK (reply_count >= 0),
    counter_version BIGINT NOT NULL DEFAULT 0 CHECK (counter_version >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
) WITH (fillfactor = 80);

-- 3. 一级点评点赞明细。
CREATE TABLE foodtime.dish_review_likes (
    review_id UUID NOT NULL REFERENCES foodtime.dish_reviews(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (review_id, user_id)
);

-- 4. 楼内回复。client_request_id 是同一发布操作跨重试保持不变的 UUID。
CREATE TABLE foodtime.dish_review_replies (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES foodtime.dish_reviews(id) ON DELETE RESTRICT,
    reply_to_id UUID,
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    client_request_id UUID NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewed_by_user_id UUID REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    reviewed_by_ai_model TEXT,
    reviewed_at TIMESTAMPTZ,
    review_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_dish_review_replies_request UNIQUE (user_id, client_request_id),
    CONSTRAINT uq_dish_review_replies_scope UNIQUE (review_id, id),
    CONSTRAINT fk_dish_review_replies_same_review FOREIGN KEY (review_id, reply_to_id)
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
    ON foodtime.dish_review_replies(review_id, created_at ASC, id ASC)
    WHERE status = 'approved' AND deleted_at IS NULL;
CREATE INDEX ix_dish_review_replies_pending_page
    ON foodtime.dish_review_replies(created_at ASC, id ASC)
    WHERE status = 'pending' AND deleted_at IS NULL;
CREATE INDEX ix_dish_review_replies_owner_page
    ON foodtime.dish_review_replies(user_id, created_at DESC, id ASC) WHERE deleted_at IS NULL;
CREATE INDEX ix_dish_review_replies_target
    ON foodtime.dish_review_replies(review_id, reply_to_id) WHERE reply_to_id IS NOT NULL;

-- 5. 回复点赞统计：回复点赞不写一级点评统计或菜品统计。
CREATE TABLE foodtime.dish_review_reply_stats (
    reply_id UUID PRIMARY KEY REFERENCES foodtime.dish_review_replies(id) ON DELETE CASCADE,
    like_count BIGINT NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    counter_version BIGINT NOT NULL DEFAULT 0 CHECK (counter_version >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
) WITH (fillfactor = 80);

-- 6. 回复点赞明细。
CREATE TABLE foodtime.dish_review_reply_likes (
    reply_id UUID NOT NULL REFERENCES foodtime.dish_review_replies(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (reply_id, user_id)
);

-- 一次性回填正式点评；V14 没有点评互动明细，互动计数从零开始。
INSERT INTO foodtime.dish_stats(dish_id, review_count, rating_sum)
SELECT d.id, COUNT(r.id), COALESCE(SUM(r.rating), 0)::BIGINT
FROM foodtime.dishes d
LEFT JOIN foodtime.dish_reviews r ON r.dish_id = d.id AND r.deleted_at IS NULL
GROUP BY d.id;
INSERT INTO foodtime.dish_review_stats(review_id) SELECT id FROM foodtime.dish_reviews;

CREATE FUNCTION foodtime.initialize_dish_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO foodtime.dish_stats(dish_id) VALUES (NEW.id);
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dishes_initialize_stats AFTER INSERT ON foodtime.dishes
    FOR EACH ROW EXECUTE FUNCTION foodtime.initialize_dish_stats();

-- 点评身份与首次发布时间不变；已删除点评不可重新启用或编辑。
CREATE FUNCTION foodtime.guard_dish_review_update()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id OR NEW.dish_id IS DISTINCT FROM OLD.dish_id
        OR NEW.user_id IS DISTINCT FROM OLD.user_id OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'Review identity and first publication time are immutable' USING ERRCODE = '23514';
    END IF;
    IF OLD.deleted_at IS NOT NULL AND (
        NEW.deleted_at IS DISTINCT FROM OLD.deleted_at OR NEW.rating IS DISTINCT FROM OLD.rating
        OR NEW.content IS DISTINCT FROM OLD.content OR NEW.submission_id IS DISTINCT FROM OLD.submission_id
    ) THEN
        RAISE EXCEPTION 'Deleted review cannot be restored or edited' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_reviews_guard BEFORE UPDATE ON foodtime.dish_reviews
    FOR EACH ROW EXECUTE FUNCTION foodtime.guard_dish_review_update();

-- 正式点评变化实时影响菜品评论数/评分总和；正文编辑不更新统计。
CREATE FUNCTION foodtime.adjust_dish_rating_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    changed_dish_id UUID;
    old_count BIGINT := 0;
    new_count BIGINT := 0;
    old_sum BIGINT := 0;
    new_sum BIGINT := 0;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        changed_dish_id := OLD.dish_id;
        IF OLD.deleted_at IS NULL THEN old_count := 1; old_sum := OLD.rating; END IF;
    END IF;
    IF TG_OP <> 'DELETE' THEN
        changed_dish_id := NEW.dish_id;
        IF NEW.deleted_at IS NULL THEN new_count := 1; new_sum := NEW.rating; END IF;
    END IF;
    IF TG_OP = 'INSERT' THEN
        INSERT INTO foodtime.dish_review_stats(review_id) VALUES (NEW.id);
    END IF;
    IF new_count <> old_count OR new_sum <> old_sum THEN
        UPDATE foodtime.dish_stats
        SET review_count = review_count + new_count - old_count,
            rating_sum = rating_sum + new_sum - old_sum,
            counter_version = counter_version + 1, updated_at = statement_timestamp()
        WHERE dish_id = changed_dish_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Missing stats row for dish %', changed_dish_id USING ERRCODE = '23503';
        END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_reviews_rating_stats AFTER INSERT OR UPDATE OR DELETE ON foodtime.dish_reviews
    FOR EACH ROW EXECUTE FUNCTION foodtime.adjust_dish_rating_stats();

CREATE FUNCTION foodtime.guard_dish_review_reply_update()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id OR NEW.review_id IS DISTINCT FROM OLD.review_id
        OR NEW.reply_to_id IS DISTINCT FROM OLD.reply_to_id OR NEW.user_id IS DISTINCT FROM OLD.user_id
        OR NEW.client_request_id IS DISTINCT FROM OLD.client_request_id
        OR NEW.content IS DISTINCT FROM OLD.content OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
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
CREATE TRIGGER trg_dish_review_replies_guard BEFORE UPDATE ON foodtime.dish_review_replies
    FOR EACH ROW EXECUTE FUNCTION foodtime.guard_dish_review_reply_update();
CREATE TRIGGER trg_dish_review_replies_updated_at BEFORE UPDATE ON foodtime.dish_review_replies
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();

CREATE FUNCTION foodtime.adjust_dish_review_reply_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    changed_review_id UUID;
    old_count BIGINT := 0;
    new_count BIGINT := 0;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        changed_review_id := OLD.review_id;
        IF OLD.status = 'approved' AND OLD.deleted_at IS NULL THEN old_count := 1; END IF;
    END IF;
    IF TG_OP <> 'DELETE' THEN
        changed_review_id := NEW.review_id;
        IF NEW.status = 'approved' AND NEW.deleted_at IS NULL THEN new_count := 1; END IF;
    END IF;
    IF TG_OP = 'INSERT' THEN
        INSERT INTO foodtime.dish_review_reply_stats(reply_id) VALUES (NEW.id);
    END IF;
    IF new_count <> old_count THEN
        UPDATE foodtime.dish_review_stats
        SET reply_count = reply_count + new_count - old_count,
            counter_version = counter_version + 1, updated_at = statement_timestamp()
        WHERE review_id = changed_review_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Missing stats row for review %', changed_review_id USING ERRCODE = '23503';
        END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_review_replies_stats AFTER INSERT OR UPDATE OR DELETE ON foodtime.dish_review_replies
    FOR EACH ROW EXECUTE FUNCTION foodtime.adjust_dish_review_reply_stats();

CREATE FUNCTION foodtime.adjust_dish_review_like_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    changed_review_id UUID;
    delta BIGINT;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Like rows are immutable; use INSERT or DELETE' USING ERRCODE = '23514';
    ELSIF TG_OP = 'INSERT' THEN changed_review_id := NEW.review_id; delta := 1;
    ELSE changed_review_id := OLD.review_id; delta := -1;
    END IF;
    UPDATE foodtime.dish_review_stats
    SET like_count = like_count + delta, counter_version = counter_version + 1,
        updated_at = statement_timestamp()
    WHERE review_id = changed_review_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Missing stats row for review %', changed_review_id USING ERRCODE = '23503';
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_review_likes_stats AFTER INSERT OR UPDATE OR DELETE ON foodtime.dish_review_likes
    FOR EACH ROW EXECUTE FUNCTION foodtime.adjust_dish_review_like_stats();

CREATE FUNCTION foodtime.adjust_dish_review_reply_like_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    changed_reply_id UUID;
    delta BIGINT;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Reply like rows are immutable; use INSERT or DELETE' USING ERRCODE = '23514';
    ELSIF TG_OP = 'INSERT' THEN changed_reply_id := NEW.reply_id; delta := 1;
    ELSE changed_reply_id := OLD.reply_id; delta := -1;
    END IF;
    UPDATE foodtime.dish_review_reply_stats
    SET like_count = like_count + delta, counter_version = counter_version + 1,
        updated_at = statement_timestamp()
    WHERE reply_id = changed_reply_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Missing stats row for reply %', changed_reply_id USING ERRCODE = '23503';
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_review_reply_likes_stats AFTER INSERT OR UPDATE OR DELETE ON foodtime.dish_review_reply_likes
    FOR EACH ROW EXECUTE FUNCTION foodtime.adjust_dish_review_reply_like_stats();

-- 统一审核日志及 AI 结论接入回复类型。
ALTER TABLE foodtime.content_moderation_logs ADD COLUMN dish_review_reply_id UUID
    REFERENCES foodtime.dish_review_replies(id) ON DELETE RESTRICT;
ALTER TABLE foodtime.content_moderation_logs DROP CONSTRAINT ck_moderation_logs_one_submission;
ALTER TABLE foodtime.content_moderation_logs ADD CONSTRAINT ck_moderation_logs_one_submission CHECK (
    num_nonnulls(review_submission_id, dish_submission_id, stall_submission_id, dish_image_id,
        community_post_id, community_comment_id, dish_review_reply_id) = 1
);
CREATE INDEX ix_moderation_logs_dish_review_reply
    ON foodtime.content_moderation_logs(dish_review_reply_id) WHERE dish_review_reply_id IS NOT NULL;
ALTER TABLE foodtime.ai_moderation_results DROP CONSTRAINT ai_moderation_results_submission_type_check;
ALTER TABLE foodtime.ai_moderation_results ADD CONSTRAINT ai_moderation_results_submission_type_check CHECK (
    submission_type IN ('review', 'photo', 'community-post', 'community-image',
        'community-comment', 'dish-review-reply')
);

-- 普通视图替代每日物化视图；每次读的是统计行，只有除法，没有评论明细聚合。
-- 执行前必须移除/停用 DishRatingSummaryRefresher，普通视图不能 REFRESH MATERIALIZED VIEW。
DROP MATERIALIZED VIEW foodtime.dish_rating_summary;
CREATE VIEW foodtime.dish_rating_summary AS
SELECT dish_id,
       CASE WHEN review_count = 0 THEN NULL::NUMERIC(3,2)
            ELSE ROUND(rating_sum::NUMERIC / review_count, 2)::NUMERIC(3,2)
       END AS average_score,
       review_count
FROM foodtime.dish_stats;

COMMENT ON TABLE foodtime.dish_stats IS '菜品实时评分汇总；只计未删除正式点评；写事务维护';
COMMENT ON TABLE foodtime.dish_review_stats IS '一级点评互动计数；点评删除隐藏整楼，不清零内部历史计数';
COMMENT ON TABLE foodtime.dish_review_likes IS '一级点评点赞明细；同用户同点评唯一';
COMMENT ON TABLE foodtime.dish_review_replies IS '楼内回复；自带审核及软删除，支持操作ID去重';
COMMENT ON TABLE foodtime.dish_review_reply_stats IS '回复点赞计数；更新不影响正文、一级点评计数或菜品评分';
COMMENT ON TABLE foodtime.dish_review_reply_likes IS '回复点赞明细；同用户同回复唯一';
COMMENT ON COLUMN foodtime.dish_review_replies.client_request_id IS '客户端为一次回复发布生成并在重试时复用；与链路requestId不同';
COMMENT ON COLUMN foodtime.dish_review_stats.reply_count IS '本点评下approved且未删除的回复数；不含点赞或待审核回复';
COMMENT ON COLUMN foodtime.dish_reviews.deleted_at IS '软删除时间；删除立即移出菜品评分，整楼公开不可见';
COMMENT ON VIEW foodtime.dish_rating_summary IS '实时评分兼容视图；无每日刷新，无评论时average_score为NULL';

COMMIT;
