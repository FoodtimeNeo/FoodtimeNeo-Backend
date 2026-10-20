-- 回复点赞明细与独立计数；不更新一级点评统计或菜品评分。
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

LOCK TABLE foodtime.dish_review_replies IN SHARE ROW EXCLUSIVE MODE;
INSERT INTO foodtime.dish_review_reply_stats(reply_id) SELECT id FROM foodtime.dish_review_replies;
CREATE FUNCTION foodtime.initialize_dish_review_reply_stats()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO foodtime.dish_review_reply_stats(reply_id) VALUES (NEW.id);
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_dish_review_replies_initialize_stats AFTER INSERT ON foodtime.dish_review_replies
    FOR EACH ROW EXECUTE FUNCTION foodtime.initialize_dish_review_reply_stats();

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

COMMENT ON TABLE foodtime.dish_review_reply_stats IS '回复点赞事务内计数；无正文和评分更新';
COMMENT ON TABLE foodtime.dish_review_reply_likes IS '用户对回复的唯一点赞明细';
