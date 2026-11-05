-- 社区互动消息：点赞按目标聚合成一条，评论/回复各自保留一条，均关联到帖子详情。
ALTER TABLE foodtime.messages
    ADD COLUMN ref_comment_id UUID REFERENCES foodtime.community_comments(id) ON DELETE SET NULL,
    ADD COLUMN actor_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE foodtime.messages
    ADD CONSTRAINT ck_messages_actor_count CHECK (actor_count >= 0);

COMMENT ON COLUMN foodtime.messages.ref_comment_id IS '互动消息定位到的评论；帖子级消息为空';
COMMENT ON COLUMN foodtime.messages.actor_count IS '聚合消息涉及的互动人数；普通消息为0';

CREATE UNIQUE INDEX uq_messages_notification_target ON foodtime.messages (
    user_id,
    category,
    ref_type,
    ref_id,
    COALESCE(ref_comment_id, '00000000-0000-0000-0000-000000000000'::uuid)
) WHERE category IN ('LIKE_POST', 'LIKE_COMMENT', 'COMMENT_POST', 'REPLY_COMMENT');

CREATE INDEX ix_messages_user_created ON foodtime.messages (user_id, created_at DESC, id DESC);
