-- 社区动态：一期建全表结构（动态、配图、评论、点赞、收藏），接口按切片交付。
-- 动态与评论自带审核状态，复用 AI 即审链（结论类型 community-post / community-comment /
-- community-image）与统一审核日志；点赞收藏是即时互动，不进审核。
CREATE TABLE foodtime.community_posts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    content TEXT NOT NULL,
    repost_of_id UUID REFERENCES foodtime.community_posts(id) ON DELETE RESTRICT,
    like_count INTEGER NOT NULL DEFAULT 0,
    comment_count INTEGER NOT NULL DEFAULT 0,
    repost_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewed_by_user_id UUID REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    reviewed_by_ai_model TEXT,
    reviewed_at TIMESTAMPTZ,
    review_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_community_posts_content CHECK (char_length(content) BETWEEN 1 AND 2000),
    CONSTRAINT ck_community_posts_counts CHECK (like_count >= 0 AND comment_count >= 0 AND repost_count >= 0),
    CONSTRAINT ck_community_posts_status CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT ck_community_posts_review_metadata CHECK (
        status = 'pending' OR (reviewed_at IS NOT NULL
            AND (reviewed_by_user_id IS NOT NULL OR reviewed_by_ai_model IS NOT NULL)))
);
CREATE INDEX ix_community_posts_feed ON foodtime.community_posts (status, created_at DESC, id ASC);
CREATE INDEX ix_community_posts_user ON foodtime.community_posts (user_id, created_at DESC, id ASC);

CREATE TABLE foodtime.community_post_images (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES foodtime.community_posts(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_community_post_images_sort CHECK (sort_order >= 0)
);
CREATE INDEX ix_community_post_images_post ON foodtime.community_post_images (post_id, sort_order, id ASC);

CREATE TABLE foodtime.community_comments (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES foodtime.community_posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    content TEXT NOT NULL,
    like_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewed_by_user_id UUID REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    reviewed_by_ai_model TEXT,
    reviewed_at TIMESTAMPTZ,
    review_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_community_comments_content CHECK (char_length(content) BETWEEN 1 AND 500),
    CONSTRAINT ck_community_comments_counts CHECK (like_count >= 0),
    CONSTRAINT ck_community_comments_status CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT ck_community_comments_review_metadata CHECK (
        status = 'pending' OR (reviewed_at IS NOT NULL
            AND (reviewed_by_user_id IS NOT NULL OR reviewed_by_ai_model IS NOT NULL)))
);
CREATE INDEX ix_community_comments_post ON foodtime.community_comments (post_id, status, created_at DESC, id ASC);
CREATE INDEX ix_community_comments_user ON foodtime.community_comments (user_id, created_at DESC, id ASC);

-- 通用点赞：target_type 预留 post / comment，菜品赞以后也能进这张表。
CREATE TABLE foodtime.community_likes (
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    target_type VARCHAR(20) NOT NULL,
    target_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_community_likes PRIMARY KEY (user_id, target_type, target_id),
    CONSTRAINT ck_community_likes_type CHECK (target_type IN ('post', 'comment'))
);
CREATE INDEX ix_community_likes_target ON foodtime.community_likes (target_type, target_id);

CREATE TABLE foodtime.community_favorites (
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE RESTRICT,
    post_id UUID NOT NULL REFERENCES foodtime.community_posts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_community_favorites PRIMARY KEY (user_id, post_id)
);

-- 统一审核日志接社区两类目标；actor 机制不变，核验继续在一个页面。
ALTER TABLE foodtime.content_moderation_logs
    ADD COLUMN community_post_id UUID REFERENCES foodtime.community_posts(id) ON DELETE RESTRICT,
    ADD COLUMN community_comment_id UUID REFERENCES foodtime.community_comments(id) ON DELETE RESTRICT;
ALTER TABLE foodtime.content_moderation_logs DROP CONSTRAINT ck_moderation_logs_one_submission;
ALTER TABLE foodtime.content_moderation_logs ADD CONSTRAINT ck_moderation_logs_one_submission
    CHECK (num_nonnulls(review_submission_id, dish_submission_id, stall_submission_id,
        dish_image_id, community_post_id, community_comment_id) = 1);
CREATE INDEX ix_moderation_logs_community_post ON foodtime.content_moderation_logs (community_post_id)
    WHERE community_post_id IS NOT NULL;
CREATE INDEX ix_moderation_logs_community_comment ON foodtime.content_moderation_logs (community_comment_id)
    WHERE community_comment_id IS NOT NULL;

COMMENT ON TABLE foodtime.community_posts IS '社区动态：自带审核状态，AI自动或人工终审；计数冗余保证feed读取';
COMMENT ON COLUMN foodtime.community_posts.repost_of_id IS '转发来源动态ID；原创为空，转发即一条轻量动态';
COMMENT ON TABLE foodtime.community_likes IS '社区点赞：(用户,类型,目标)唯一；target_type预留扩展';
