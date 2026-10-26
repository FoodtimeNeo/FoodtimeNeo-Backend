-- 系统公告与个人消息收件箱：notices 全员广播（seq 仅作轮询游标，不对外当身份），notice_reads 记录已读，messages 为个人消息。
CREATE TABLE foodtime.notices (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    seq BIGSERIAL NOT NULL UNIQUE,
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES foodtime.users(id)
);
COMMENT ON TABLE foodtime.notices IS '系统公告：全员广播，seq 仅作轮询游标，不对外当身份';
COMMENT ON COLUMN foodtime.notices.id IS '公告主键 UUID';
COMMENT ON COLUMN foodtime.notices.title IS '公告标题';
COMMENT ON COLUMN foodtime.notices.body IS '公告正文';
COMMENT ON COLUMN foodtime.notices.seq IS '单调递增轮询游标，唯一，不对外当身份';
COMMENT ON COLUMN foodtime.notices.published_at IS '发布时间';
COMMENT ON COLUMN foodtime.notices.created_by IS '发布人，引用 foodtime.users(id)';

CREATE TABLE foodtime.notice_reads (
    notice_id UUID NOT NULL REFERENCES foodtime.notices(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES foodtime.users(id),
    read_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (notice_id, user_id)
);
COMMENT ON TABLE foodtime.notice_reads IS '公告已读记录：用户与公告的多对多已读标记';
COMMENT ON COLUMN foodtime.notice_reads.notice_id IS '公告 ID，级联删除';
COMMENT ON COLUMN foodtime.notice_reads.user_id IS '已读用户 ID';
COMMENT ON COLUMN foodtime.notice_reads.read_at IS '已读时间';
CREATE INDEX ix_notice_reads_user ON foodtime.notice_reads (user_id, notice_id);

CREATE TABLE foodtime.messages (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES foodtime.users(id),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'SYSTEM',
    ref_type TEXT NULL,
    ref_id UUID NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE foodtime.messages IS '个人消息收件箱：面向单用户的站内信';
COMMENT ON COLUMN foodtime.messages.id IS '消息主键 UUID';
COMMENT ON COLUMN foodtime.messages.user_id IS '收件用户 ID';
COMMENT ON COLUMN foodtime.messages.title IS '消息标题';
COMMENT ON COLUMN foodtime.messages.body IS '消息正文';
COMMENT ON COLUMN foodtime.messages.category IS '消息分类，默认 SYSTEM';
COMMENT ON COLUMN foodtime.messages.ref_type IS '关联业务类型，可为空';
COMMENT ON COLUMN foodtime.messages.ref_id IS '关联业务 ID，可为空';
COMMENT ON COLUMN foodtime.messages.is_read IS '是否已读，默认 FALSE';
COMMENT ON COLUMN foodtime.messages.created_at IS '创建时间';
CREATE INDEX ix_messages_user ON foodtime.messages (user_id, id);
CREATE INDEX ix_messages_user_unread ON foodtime.messages (user_id) WHERE NOT is_read;
