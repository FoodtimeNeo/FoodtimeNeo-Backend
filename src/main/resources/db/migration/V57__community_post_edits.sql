ALTER TABLE foodtime.community_posts
    ADD COLUMN content_revision bigint NOT NULL DEFAULT 1 CHECK(content_revision>=1),
    ADD COLUMN edited_at timestamptz,
    ADD COLUMN repost_counted boolean NOT NULL DEFAULT false;
UPDATE foodtime.community_posts SET repost_counted=true WHERE repost_of_id IS NOT NULL AND status='approved';

CREATE TABLE foodtime.community_post_versions (
    post_id uuid NOT NULL REFERENCES foodtime.community_posts(id) ON DELETE CASCADE,
    revision bigint NOT NULL,
    payload jsonb NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY(post_id,revision)
);
CREATE TABLE foodtime.community_post_edit_requests (
    post_id uuid NOT NULL REFERENCES foodtime.community_posts(id) ON DELETE CASCADE,
    client_request_id uuid NOT NULL,
    payload_hash varchar(64) NOT NULL,
    resulting_revision bigint NOT NULL,
    changed boolean NOT NULL,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY(post_id,client_request_id)
);
-- Separate immutable create receipt: editing cannot change what a retried creation submitted.
ALTER TABLE foodtime.community_posts ADD COLUMN create_payload jsonb;

CREATE FUNCTION foodtime.community_post_snapshot(target uuid) RETURNS jsonb LANGUAGE sql STABLE AS $$
    SELECT jsonb_build_object('post',to_jsonb(p)-'create_payload','images',
        COALESCE((SELECT jsonb_agg(to_jsonb(i) ORDER BY sort_order,id) FROM foodtime.community_post_images i WHERE i.post_id=p.id),'[]'),
        'ai',COALESCE((SELECT jsonb_agg(to_jsonb(a)) FROM foodtime.ai_moderation_results a
            WHERE (a.submission_type='community-post' AND a.submission_id=p.id)
               OR (a.submission_type='community-image' AND a.submission_id IN(SELECT id FROM foodtime.community_post_images WHERE post_id=p.id))),'[]'))
    FROM foodtime.community_posts p WHERE p.id=target
$$;
UPDATE foodtime.community_posts p SET create_payload=jsonb_build_object(
    'title',p.title,'content',p.content,'repostOfId',p.repost_of_id,'dishId',p.dish_id,
    'imageUrls',COALESCE((SELECT jsonb_agg(image_url ORDER BY sort_order,id) FROM foodtime.community_post_images WHERE post_id=p.id),'[]'));

-- Retain evidence of each decision even after the author replaces the pending body.
ALTER TABLE foodtime.content_moderation_logs ADD COLUMN post_content_revision bigint, ADD COLUMN post_content_snapshot jsonb;
UPDATE foodtime.content_moderation_logs l SET post_content_revision=p.content_revision,
    post_content_snapshot=foodtime.community_post_snapshot(p.id)
FROM foodtime.community_posts p WHERE l.community_post_id=p.id;
CREATE FUNCTION foodtime.snapshot_post_moderation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.community_post_id IS NOT NULL THEN
        SELECT content_revision,foodtime.community_post_snapshot(id)
        INTO NEW.post_content_revision,NEW.post_content_snapshot FROM foodtime.community_posts WHERE id=NEW.community_post_id;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER snapshot_post_moderation BEFORE INSERT ON foodtime.content_moderation_logs
FOR EACH ROW EXECUTE FUNCTION foodtime.snapshot_post_moderation();

CREATE FUNCTION foodtime.guard_editable_community_post() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE editing boolean := NEW.content_revision=OLD.content_revision+1;
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id OR NEW.user_id IS DISTINCT FROM OLD.user_id
       OR NEW.created_at IS DISTINCT FROM OLD.created_at OR NEW.client_request_id IS DISTINCT FROM OLD.client_request_id
       OR NEW.repost_of_id IS DISTINCT FROM OLD.repost_of_id OR NEW.create_payload IS DISTINCT FROM OLD.create_payload THEN
        RAISE EXCEPTION 'Post identity and creation receipt are immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.deleted_at IS NOT NULL AND (editing OR NEW.deleted_at IS DISTINCT FROM OLD.deleted_at OR NEW.status IS DISTINCT FROM OLD.status) THEN
        RAISE EXCEPTION 'Deleted posts cannot be edited or restored' USING ERRCODE='23514';
    END IF;
    IF editing THEN
        IF NEW.status<>'pending' OR NEW.reviewed_at IS NOT NULL OR NEW.reviewed_by_user_id IS NOT NULL
           OR NEW.reviewed_by_ai_model IS NOT NULL OR NEW.review_reason IS NOT NULL OR NEW.edited_at IS NULL THEN
            RAISE EXCEPTION 'Post edit must return to unreviewed pending state' USING ERRCODE='23514';
        END IF;
    ELSE
        IF NEW.content_revision<>OLD.content_revision OR NEW.edited_at IS DISTINCT FROM OLD.edited_at
           OR NEW.title IS DISTINCT FROM OLD.title OR NEW.content IS DISTINCT FROM OLD.content OR NEW.dish_id IS DISTINCT FROM OLD.dish_id THEN
            RAISE EXCEPTION 'Content changes require a new revision' USING ERRCODE='23514';
        END IF;
        IF OLD.status<>'pending' AND NEW.status IS DISTINCT FROM OLD.status THEN
            RAISE EXCEPTION 'Final verdict cannot change without editing' USING ERRCODE='23514';
        END IF;
    END IF;
    IF NEW.status<>'pending' THEN
        NEW.review_stage='completed';
        IF NEW.reviewed_at IS NULL OR num_nonnulls(NEW.reviewed_by_user_id,NEW.reviewed_by_ai_model)<>1 THEN
            RAISE EXCEPTION 'Exactly one final reviewer is required' USING ERRCODE='23514';
        END IF;
    END IF;
    NEW.updated_at=statement_timestamp();
    RETURN NEW;
END;
$$;
DROP TRIGGER trg_community_posts_guard ON foodtime.community_posts;
CREATE TRIGGER trg_community_posts_guard BEFORE UPDATE ON foodtime.community_posts
FOR EACH ROW EXECUTE FUNCTION foodtime.guard_editable_community_post();
