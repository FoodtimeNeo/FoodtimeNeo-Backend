ALTER TABLE foodtime.community_posts
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0 CHECK (view_count >= 0);

CREATE TABLE foodtime.community_post_views (
    post_id UUID NOT NULL REFERENCES foodtime.community_posts(id) ON DELETE CASCADE,
    viewer_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    client_request_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, viewer_id, client_request_id)
);
COMMENT ON TABLE foodtime.community_post_views IS '按详情打开请求去重；重试不重复累计浏览量';
