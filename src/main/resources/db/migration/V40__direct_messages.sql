-- 1:1 私信：会话按用户 UUID 排序后唯一，双方各自维护已读与删除位置。
CREATE TABLE foodtime.conversations (
    id UUID PRIMARY KEY,
    user_low UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    user_high UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    low_last_read_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    high_last_read_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    low_deleted_at TIMESTAMPTZ,
    high_deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_conversations_order CHECK (user_low < user_high)
);
CREATE UNIQUE INDEX uq_conversations_pair ON foodtime.conversations(user_low, user_high);
CREATE INDEX ix_conversations_low ON foodtime.conversations(user_low, updated_at DESC);
CREATE INDEX ix_conversations_high ON foodtime.conversations(user_high, updated_at DESC);
COMMENT ON TABLE foodtime.conversations IS '一对一私信会话';
COMMENT ON COLUMN foodtime.conversations.user_low IS 'UUID 较小的参与人';
COMMENT ON COLUMN foodtime.conversations.user_high IS 'UUID 较大的参与人';
COMMENT ON COLUMN foodtime.conversations.low_last_read_at IS 'user_low 的最后阅读时间';
COMMENT ON COLUMN foodtime.conversations.low_deleted_at IS 'user_low 隐藏会话的时间；新消息会恢复';

CREATE TABLE foodtime.direct_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES foodtime.conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES foodtime.users(id),
    content TEXT NOT NULL,
    client_request_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_direct_messages_content CHECK (char_length(btrim(content)) BETWEEN 1 AND 1000),
    CONSTRAINT uq_direct_messages_client UNIQUE (sender_id, client_request_id)
);
CREATE INDEX ix_direct_messages_conversation
    ON foodtime.direct_messages(conversation_id, created_at DESC, id DESC);
COMMENT ON TABLE foodtime.direct_messages IS '一对一私信正文；只保存纯文本';
