ALTER TABLE foodtime.community_comments
    ADD COLUMN client_request_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN root_comment_id UUID REFERENCES foodtime.community_comments(id) ON DELETE RESTRICT,
    ADD COLUMN reply_to_comment_id UUID REFERENCES foodtime.community_comments(id) ON DELETE RESTRICT,
    ADD COLUMN reply_to_user_id UUID REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    ADD COLUMN reply_count BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT uq_community_comment_request UNIQUE(user_id,client_request_id),
    ADD CONSTRAINT ck_community_comment_thread CHECK (
        (root_comment_id IS NULL AND reply_to_comment_id IS NULL AND reply_to_user_id IS NULL)
        OR (root_comment_id IS NOT NULL AND reply_to_comment_id IS NOT NULL AND reply_to_user_id IS NOT NULL
            AND root_comment_id<>id AND reply_to_comment_id<>id)),
    ADD CONSTRAINT ck_community_comment_reply_count CHECK(reply_count>=0);
ALTER TABLE foodtime.community_comments ALTER COLUMN like_count TYPE BIGINT;

CREATE FUNCTION foodtime.guard_community_thread() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE root foodtime.community_comments; target foodtime.community_comments;
BEGIN
    IF TG_OP='UPDATE' THEN
        IF (NEW.post_id,NEW.root_comment_id,NEW.reply_to_comment_id,NEW.reply_to_user_id,NEW.client_request_id)
            IS DISTINCT FROM (OLD.post_id,OLD.root_comment_id,OLD.reply_to_comment_id,OLD.reply_to_user_id,OLD.client_request_id) THEN
            RAISE EXCEPTION 'Comment thread is immutable' USING ERRCODE='23514';
        END IF;
        IF NEW.status IS NOT DISTINCT FROM OLD.status THEN RETURN NEW; END IF;
    END IF;
    PERFORM id FROM foodtime.community_posts WHERE id=NEW.post_id AND status='approved' AND deleted_at IS NULL FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Post unavailable' USING ERRCODE='23514'; END IF;
    IF NEW.root_comment_id IS NOT NULL THEN
        SELECT * INTO root FROM foodtime.community_comments WHERE id=NEW.root_comment_id FOR UPDATE;
        IF NOT FOUND OR root.root_comment_id IS NOT NULL OR root.post_id<>NEW.post_id OR root.status<>'approved'
            OR (TG_OP='INSERT' AND root.deleted_at IS NOT NULL) THEN
            RAISE EXCEPTION 'Reply root unavailable' USING ERRCODE='23514';
        END IF;
        SELECT * INTO target FROM foodtime.community_comments WHERE id=NEW.reply_to_comment_id;
        IF NOT FOUND OR target.post_id<>NEW.post_id OR COALESCE(target.root_comment_id,target.id)<>root.id
            OR target.user_id<>NEW.reply_to_user_id OR target.status<>'approved'
            OR (TG_OP='INSERT' AND target.deleted_at IS NOT NULL) THEN
            RAISE EXCEPTION 'Reply target must belong to the same thread' USING ERRCODE='23514';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_community_thread BEFORE INSERT OR UPDATE ON foodtime.community_comments
    FOR EACH ROW EXECUTE FUNCTION foodtime.guard_community_thread();

CREATE FUNCTION foodtime.count_community_comments() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE delta INTEGER; post UUID; root UUID;
BEGIN
    delta=CASE WHEN TG_OP<>'DELETE' AND NEW.status='approved' AND NEW.deleted_at IS NULL THEN 1 ELSE 0 END
        -CASE WHEN TG_OP<>'INSERT' AND OLD.status='approved' AND OLD.deleted_at IS NULL THEN 1 ELSE 0 END;
    post=CASE WHEN TG_OP='DELETE' THEN OLD.post_id ELSE NEW.post_id END;
    root=CASE WHEN TG_OP='DELETE' THEN OLD.root_comment_id ELSE NEW.root_comment_id END;
    IF delta<>0 THEN
        UPDATE foodtime.community_posts SET comment_count=comment_count+delta WHERE id=post;
        IF root IS NOT NULL THEN UPDATE foodtime.community_comments SET reply_count=reply_count+delta WHERE id=root; END IF;
    END IF;
    RETURN NULL;
END;
$$;
CREATE TRIGGER trg_community_comment_counts AFTER INSERT OR DELETE OR UPDATE OF status,deleted_at ON foodtime.community_comments
    FOR EACH ROW EXECUTE FUNCTION foodtime.count_community_comments();
UPDATE foodtime.community_posts p SET comment_count=(SELECT count(*) FROM foodtime.community_comments c
    WHERE c.post_id=p.id AND c.status='approved' AND c.deleted_at IS NULL);
CREATE INDEX ix_community_roots ON foodtime.community_comments(post_id,created_at DESC,id DESC) WHERE root_comment_id IS NULL;
CREATE INDEX ix_community_thread_replies ON foodtime.community_comments(root_comment_id,created_at,id) WHERE deleted_at IS NULL;
COMMENT ON COLUMN foodtime.community_comments.root_comment_id IS '一级评论为空；楼内所有回复都指向同一个一级评论，仅缩进一层';
COMMENT ON COLUMN foodtime.community_comments.reply_to_user_id IS '由服务端按目标评论推导，禁止客户端伪造被回复用户';
