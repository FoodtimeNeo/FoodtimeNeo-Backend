-- Real-time scores and soft-deleted reviews; replaces the daily materialized snapshot.
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

INSERT INTO foodtime.dish_stats(dish_id, review_count, rating_sum)
SELECT d.id, COUNT(r.id), COALESCE(SUM(r.rating), 0)::BIGINT
FROM foodtime.dishes d
LEFT JOIN foodtime.dish_reviews r ON r.dish_id = d.id AND r.deleted_at IS NULL
GROUP BY d.id;
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

DROP MATERIALIZED VIEW foodtime.dish_rating_summary;
CREATE VIEW foodtime.dish_rating_summary AS
SELECT dish_id,
       CASE WHEN review_count = 0 THEN NULL::NUMERIC(3,2)
            ELSE ROUND(rating_sum::NUMERIC / review_count, 2)::NUMERIC(3,2)
       END AS average_score,
       review_count
FROM foodtime.dish_stats;
