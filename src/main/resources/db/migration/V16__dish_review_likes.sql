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

INSERT INTO foodtime.dish_review_stats(review_id) SELECT id FROM foodtime.dish_reviews;
CREATE FUNCTION foodtime.initialize_dish_review_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO foodtime.dish_review_stats(review_id) VALUES (NEW.id);
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_reviews_initialize_stats AFTER INSERT ON foodtime.dish_reviews
    FOR EACH ROW EXECUTE FUNCTION foodtime.initialize_dish_review_stats();
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
