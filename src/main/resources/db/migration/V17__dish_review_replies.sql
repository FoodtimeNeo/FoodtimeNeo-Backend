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
