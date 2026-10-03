-- Source: docs/database-design.md. UUIDs are supplied by the application.
-- Use schema-qualified names so this migration is independent of search_path.

CREATE TABLE foodtime.users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash TEXT NOT NULL,
    email_verified_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    display_name VARCHAR(50) NOT NULL,
    avatar_url TEXT,
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email))),
    CONSTRAINT ck_users_status CHECK (status IN ('active', 'disabled')),
    CONSTRAINT ck_users_role CHECK (role IN ('user', 'admin', 'superadmin'))
);
CREATE UNIQUE INDEX ux_users_normalized_email ON foodtime.users (lower(btrim(email)));
COMMENT ON TABLE foodtime.users IS '用户表：邮箱账号、密码哈希及角色';
COMMENT ON COLUMN foodtime.users.password_hash IS '仅存密码哈希，不存明文密码';
COMMENT ON COLUMN foodtime.users.role IS 'user / admin / superadmin；角色授权由服务端校验';

CREATE TABLE foodtime.dining_halls (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    cover_image_url TEXT,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_dining_halls_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT ck_dining_halls_coordinates CHECK ((latitude IS NULL) = (longitude IS NULL))
);
COMMENT ON TABLE foodtime.dining_halls IS '食堂表';

CREATE TABLE foodtime.stalls (
    id UUID PRIMARY KEY,
    dining_hall_id UUID NOT NULL REFERENCES foodtime.dining_halls(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    cover_image_url TEXT,
    floor VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_stalls_hall_name UNIQUE (dining_hall_id, name),
    CONSTRAINT ck_stalls_status CHECK (status IN ('active', 'inactive'))
);
COMMENT ON TABLE foodtime.stalls IS '档口表：同一食堂内名称唯一';

CREATE TABLE foodtime.dishes (
    id UUID PRIMARY KEY,
    stall_id UUID NOT NULL REFERENCES foodtime.stalls(id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    price NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_dishes_price CHECK (price >= 0),
    CONSTRAINT ck_dishes_status CHECK (status IN ('active', 'inactive'))
);
CREATE INDEX ix_dishes_stall_id ON foodtime.dishes (stall_id);
COMMENT ON TABLE foodtime.dishes IS '菜品表';

CREATE TABLE foodtime.dish_images (
    id UUID PRIMARY KEY,
    dish_id UUID NOT NULL REFERENCES foodtime.dishes(id) ON DELETE RESTRICT,
    uploaded_by_user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    image_url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_dish_images_status CHECK (status IN ('pending', 'approved', 'rejected'))
);
CREATE INDEX ix_dish_images_dish_id ON foodtime.dish_images (dish_id);
CREATE INDEX ix_dish_images_uploaded_by ON foodtime.dish_images (uploaded_by_user_id);
COMMENT ON TABLE foodtime.dish_images IS '菜品图片表：保留上传人与审核状态';

CREATE TABLE foodtime.dish_review_submissions (
    id UUID PRIMARY KEY,
    dish_id UUID NOT NULL REFERENCES foodtime.dishes(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    rating SMALLINT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by_user_id UUID REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    reviewed_at TIMESTAMPTZ,
    review_reason TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_review_submissions_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_review_submissions_status CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT ck_review_submissions_review_metadata CHECK (
        status = 'pending' OR (reviewed_by_user_id IS NOT NULL AND reviewed_at IS NOT NULL)
    )
);
CREATE UNIQUE INDEX ux_review_submissions_pending
    ON foodtime.dish_review_submissions (dish_id, user_id) WHERE status = 'pending';
-- The partial unique index does not cover historical (non-pending) foreign-key references.
CREATE INDEX ix_review_submissions_dish_id ON foodtime.dish_review_submissions (dish_id);
CREATE INDEX ix_review_submissions_user_id ON foodtime.dish_review_submissions (user_id);
CREATE INDEX ix_review_submissions_reviewed_by ON foodtime.dish_review_submissions (reviewed_by_user_id)
    WHERE reviewed_by_user_id IS NOT NULL;
COMMENT ON TABLE foodtime.dish_review_submissions IS '菜品评论审核表：保留历史，同一用户对同一菜品最多一条待审核记录';

CREATE TABLE foodtime.dish_reviews (
    id UUID PRIMARY KEY,
    dish_id UUID NOT NULL REFERENCES foodtime.dishes(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    rating SMALLINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_dish_reviews_dish_user UNIQUE (dish_id, user_id),
    CONSTRAINT ck_dish_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
CREATE INDEX ix_dish_reviews_user_id ON foodtime.dish_reviews (user_id);
COMMENT ON TABLE foodtime.dish_reviews IS '正式菜品评论表：只保存已审核通过的评论';
COMMENT ON COLUMN foodtime.dish_reviews.created_at IS '由审核业务复制原提交时间；直接插入时默认 now()';

CREATE TABLE foodtime.stall_submissions (
    id UUID PRIMARY KEY,
    dining_hall_id UUID NOT NULL REFERENCES foodtime.dining_halls(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    cover_image_url TEXT NOT NULL,
    submitted_by_user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewed_by_user_id UUID REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    reviewed_at TIMESTAMPTZ,
    review_reason TEXT,
    approved_stall_id UUID UNIQUE REFERENCES foodtime.stalls(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_stall_submissions_status CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT ck_stall_submissions_approved_link CHECK ((status = 'approved') = (approved_stall_id IS NOT NULL)),
    CONSTRAINT ck_stall_submissions_review_metadata CHECK (
        status = 'pending' OR (reviewed_by_user_id IS NOT NULL AND reviewed_at IS NOT NULL)
    )
);
CREATE INDEX ix_stall_submissions_hall_id ON foodtime.stall_submissions (dining_hall_id);
CREATE INDEX ix_stall_submissions_submitted_by ON foodtime.stall_submissions (submitted_by_user_id);
CREATE INDEX ix_stall_submissions_reviewed_by ON foodtime.stall_submissions (reviewed_by_user_id)
    WHERE reviewed_by_user_id IS NOT NULL;
COMMENT ON TABLE foodtime.stall_submissions IS '档口提交审核表';

CREATE TABLE foodtime.dish_submissions (
    id UUID PRIMARY KEY,
    stall_id UUID NOT NULL REFERENCES foodtime.stalls(id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    price NUMERIC(10,2),
    image_url TEXT NOT NULL,
    submitted_by_user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewed_by_user_id UUID REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    reviewed_at TIMESTAMPTZ,
    review_reason TEXT,
    approved_dish_id UUID UNIQUE REFERENCES foodtime.dishes(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_dish_submissions_price CHECK (price >= 0),
    CONSTRAINT ck_dish_submissions_status CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT ck_dish_submissions_approved_link CHECK ((status = 'approved') = (approved_dish_id IS NOT NULL)),
    CONSTRAINT ck_dish_submissions_review_metadata CHECK (
        status = 'pending' OR (reviewed_by_user_id IS NOT NULL AND reviewed_at IS NOT NULL)
    ),
    CONSTRAINT ck_dish_submissions_approved_price CHECK (status <> 'approved' OR price IS NOT NULL)
);
CREATE INDEX ix_dish_submissions_stall_id ON foodtime.dish_submissions (stall_id);
CREATE INDEX ix_dish_submissions_submitted_by ON foodtime.dish_submissions (submitted_by_user_id);
CREATE INDEX ix_dish_submissions_reviewed_by ON foodtime.dish_submissions (reviewed_by_user_id)
    WHERE reviewed_by_user_id IS NOT NULL;
COMMENT ON TABLE foodtime.dish_submissions IS '菜品提交审核表：待审核时价格可空，通过前必须补齐';

CREATE TABLE foodtime.content_moderation_logs (
    id UUID PRIMARY KEY,
    review_submission_id UUID REFERENCES foodtime.dish_review_submissions(id) ON DELETE RESTRICT,
    dish_submission_id UUID REFERENCES foodtime.dish_submissions(id) ON DELETE RESTRICT,
    stall_submission_id UUID REFERENCES foodtime.stall_submissions(id) ON DELETE RESTRICT,
    moderator_user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    action VARCHAR(20) NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_moderation_logs_action CHECK (action IN ('approve', 'reject')),
    CONSTRAINT ck_moderation_logs_one_submission CHECK (
        num_nonnulls(review_submission_id, dish_submission_id, stall_submission_id) = 1
    )
);
CREATE INDEX ix_moderation_logs_review_submission ON foodtime.content_moderation_logs (review_submission_id)
    WHERE review_submission_id IS NOT NULL;
CREATE INDEX ix_moderation_logs_dish_submission ON foodtime.content_moderation_logs (dish_submission_id)
    WHERE dish_submission_id IS NOT NULL;
CREATE INDEX ix_moderation_logs_stall_submission ON foodtime.content_moderation_logs (stall_submission_id)
    WHERE stall_submission_id IS NOT NULL;
CREATE INDEX ix_moderation_logs_moderator ON foodtime.content_moderation_logs (moderator_user_id);
COMMENT ON TABLE foodtime.content_moderation_logs IS '内容审核日志表：每条日志必须且只能关联一种审核提交';

-- Keep the last-modified time correct for updates from both the application and SQL clients.
CREATE FUNCTION foodtime.set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := statement_timestamp();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON foodtime.users
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
CREATE TRIGGER trg_dining_halls_updated_at BEFORE UPDATE ON foodtime.dining_halls
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
CREATE TRIGGER trg_stalls_updated_at BEFORE UPDATE ON foodtime.stalls
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
CREATE TRIGGER trg_dishes_updated_at BEFORE UPDATE ON foodtime.dishes
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
CREATE TRIGGER trg_review_submissions_updated_at BEFORE UPDATE ON foodtime.dish_review_submissions
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
CREATE TRIGGER trg_dish_reviews_updated_at BEFORE UPDATE ON foodtime.dish_reviews
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
CREATE TRIGGER trg_stall_submissions_updated_at BEFORE UPDATE ON foodtime.stall_submissions
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
CREATE TRIGGER trg_dish_submissions_updated_at BEFORE UPDATE ON foodtime.dish_submissions
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();

-- Include dishes with no approved reviews: average_score is NULL and review_count is 0.
-- WITH DATA is required before the first CONCURRENTLY refresh, even for an empty database.
CREATE MATERIALIZED VIEW foodtime.dish_rating_summary AS
SELECT d.id AS dish_id,
       AVG(r.rating)::NUMERIC(3,2) AS average_score,
       COUNT(r.id) AS review_count
FROM foodtime.dishes d
LEFT JOIN foodtime.dish_reviews r ON r.dish_id = d.id
GROUP BY d.id
WITH DATA;
CREATE UNIQUE INDEX ux_dish_rating_summary_dish_id ON foodtime.dish_rating_summary (dish_id);
COMMENT ON MATERIALIZED VIEW foodtime.dish_rating_summary IS '菜品评分汇总；由应用每 24 小时并发刷新';
COMMENT ON COLUMN foodtime.dish_rating_summary.average_score IS '仅计算正式评论，无评论时为 NULL';
COMMENT ON COLUMN foodtime.dish_rating_summary.review_count IS '正式评论数量，无评论时为 0';
