-- Directed follow relation. Both sides cascade so an account removal cannot leave dangling edges;
-- users are normally soft-disabled, so this is a safety net rather than a routine path.
CREATE TABLE foodtime.user_follows (
    follower_id  UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, following_id),
    CONSTRAINT ck_user_follows_not_self CHECK (follower_id <> following_id)
);

-- The primary key already serves "who do I follow"; this index serves "who follows me".
CREATE INDEX ix_user_follows_following ON foodtime.user_follows (following_id, created_at DESC);

COMMENT ON TABLE foodtime.user_follows IS '用户关注关系：follower_id 关注 following_id，方向单向';
COMMENT ON COLUMN foodtime.user_follows.created_at IS '关注时间，用于关注/粉丝列表倒序';
