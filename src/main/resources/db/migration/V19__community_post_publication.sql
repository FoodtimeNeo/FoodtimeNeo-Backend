-- 图文社区扩展；保留旧记录及旧点赞明细，后续迁移切换互动与楼中楼。
ALTER TABLE foodtime.community_posts
    ADD COLUMN title VARCHAR(50),
    ADD COLUMN client_request_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN favorite_count BIGINT NOT NULL DEFAULT 0;
UPDATE foodtime.community_posts SET title = COALESCE(NULLIF(left(btrim(content), 50), ''), '校园食记');
ALTER TABLE foodtime.community_posts ALTER COLUMN title SET NOT NULL;
ALTER TABLE foodtime.community_posts ALTER COLUMN title SET DEFAULT '校园食记';
ALTER TABLE foodtime.community_posts ALTER COLUMN like_count TYPE BIGINT;
ALTER TABLE foodtime.community_posts ALTER COLUMN comment_count TYPE BIGINT;
ALTER TABLE foodtime.community_posts ADD CONSTRAINT uq_community_post_request UNIQUE(user_id, client_request_id);
ALTER TABLE foodtime.community_posts ADD CONSTRAINT ck_community_post_title CHECK(char_length(btrim(title)) BETWEEN 1 AND 50);

DO $$
DECLARE tab TEXT;
BEGIN
    FOREACH tab IN ARRAY ARRAY['community_posts', 'community_comments'] LOOP
        EXECUTE format('ALTER TABLE foodtime.%I
            ADD COLUMN review_stage VARCHAR(20) NOT NULL DEFAULT ''ai_pending'',
            ADD COLUMN manual_review_reason VARCHAR(32),
            ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            ADD COLUMN deleted_at TIMESTAMPTZ,
            ADD COLUMN deleted_by_user_id UUID REFERENCES foodtime.users(id) ON DELETE RESTRICT,
            ADD COLUMN delete_reason TEXT', tab);
        EXECUTE format('UPDATE foodtime.%I SET review_stage=CASE WHEN status=''pending'' THEN ''manual_pending'' ELSE ''completed'' END,
            manual_review_reason=CASE WHEN status=''pending'' THEN ''uncertain'' END', tab);
        EXECUTE format('ALTER TABLE foodtime.%I ADD CONSTRAINT %I CHECK (
            (status=''pending'' AND review_stage IN (''ai_pending'',''manual_pending''))
            OR (status IN (''approved'',''rejected'') AND review_stage=''completed''))', tab, 'ck_'||tab||'_stage');
        EXECUTE format('ALTER TABLE foodtime.%I ADD CONSTRAINT %I CHECK (
            manual_review_reason IS NULL OR manual_review_reason IN (''uncertain'',''ai_unavailable'',''ai_disabled''))',tab,'ck_'||tab||'_manual_reason');
        EXECUTE format('ALTER TABLE foodtime.%I ADD CONSTRAINT %I CHECK (
            review_stage<>''manual_pending'' OR manual_review_reason IS NOT NULL)',tab,'ck_'||tab||'_manual_required');
        EXECUTE format('ALTER TABLE foodtime.%I ADD CONSTRAINT %I CHECK (
            (deleted_at IS NULL) = (deleted_by_user_id IS NULL))',tab,'ck_'||tab||'_delete_actor');
        EXECUTE format('CREATE INDEX %I ON foodtime.%I(review_stage,created_at,id) WHERE status=''pending'' AND deleted_at IS NULL',
            'ix_'||tab||'_pending_stage', tab);
    END LOOP;
END;
$$;

ALTER TABLE foodtime.community_post_images ADD COLUMN width INTEGER, ADD COLUMN height INTEGER;
ALTER TABLE foodtime.community_post_images ADD CONSTRAINT ck_community_image_dimensions
    CHECK(width > 0 AND height > 0 AND width::BIGINT * height <= 20000000);
-- 不伪造旧文件的宽高。旧行允许未知，新插入/更新的行必须保存已核验的真实尺寸。
ALTER TABLE foodtime.community_post_images ADD CONSTRAINT ck_community_image_dimensions_required
    CHECK(width IS NOT NULL AND height IS NOT NULL) NOT VALID;
ALTER TABLE foodtime.community_post_images ADD CONSTRAINT ck_community_image_slots CHECK(sort_order BETWEEN 0 AND 8);
ALTER TABLE foodtime.community_post_images ADD CONSTRAINT uq_community_image_position UNIQUE(post_id,sort_order);
ALTER TABLE foodtime.community_post_images ADD CONSTRAINT uq_community_image_reference UNIQUE(post_id,image_url);

CREATE FUNCTION foodtime.guard_community_publication()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id OR NEW.user_id IS DISTINCT FROM OLD.user_id
        OR NEW.content IS DISTINCT FROM OLD.content OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'Community identity and content are immutable' USING ERRCODE='23514';
    END IF;
    IF TG_TABLE_NAME='community_posts' THEN
        IF NEW.title IS DISTINCT FROM OLD.title OR NEW.client_request_id IS DISTINCT FROM OLD.client_request_id
            OR NEW.repost_of_id IS DISTINCT FROM OLD.repost_of_id THEN
            RAISE EXCEPTION 'Published post cannot be edited' USING ERRCODE='23514';
        END IF;
    END IF;
    IF OLD.status<>'pending' AND NEW.status IS DISTINCT FROM OLD.status THEN
        RAISE EXCEPTION 'Final community verdict is immutable' USING ERRCODE='23514';
    END IF;
    IF OLD.deleted_at IS NOT NULL AND (NEW.deleted_at IS DISTINCT FROM OLD.deleted_at OR NEW.status IS DISTINCT FROM OLD.status) THEN
        RAISE EXCEPTION 'Deleted community content cannot be restored or moderated' USING ERRCODE='23514';
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
CREATE TRIGGER trg_community_posts_guard BEFORE UPDATE ON foodtime.community_posts
    FOR EACH ROW EXECUTE FUNCTION foodtime.guard_community_publication();
CREATE TRIGGER trg_community_comments_guard BEFORE UPDATE ON foodtime.community_comments
    FOR EACH ROW EXECUTE FUNCTION foodtime.guard_community_publication();
CREATE INDEX ix_community_posts_public_cursor ON foodtime.community_posts(created_at DESC,id DESC)
    WHERE status='approved' AND deleted_at IS NULL;
CREATE INDEX ix_community_posts_owner_cursor ON foodtime.community_posts(user_id,created_at DESC,id DESC)
    WHERE deleted_at IS NULL;
COMMENT ON COLUMN foodtime.community_posts.client_request_id IS '客户端单次发布 UUID；网络重试复用，禁止重复生成帖子';
COMMENT ON COLUMN foodtime.community_post_images.width IS '服务端核验宽度；仅迁移前旧记录可未知，不填伪造尺寸';
