-- 举报决定关联的服务器真实动作；不再让客户端手填“已处罚”。
CREATE TABLE foodtime.report_actions (
    id UUID PRIMARY KEY,
    decision_id UUID NOT NULL REFERENCES foodtime.report_decisions(id) ON DELETE CASCADE,
    action_type VARCHAR(24) NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id UUID NOT NULL,
    performed_by UUID REFERENCES foodtime.users(id) ON DELETE SET NULL,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_report_actions_type CHECK (action_type IN ('remove_post','remove_comment','disable_user','restore_user')),
    CONSTRAINT ck_report_actions_target CHECK (target_type IN ('post','comment','user')),
    CONSTRAINT ck_report_actions_note CHECK (note IS NULL OR char_length(note) <= 500),
    CONSTRAINT uq_report_actions_once UNIQUE (decision_id, action_type, target_id)
);
CREATE INDEX ix_report_actions_decision ON foodtime.report_actions (decision_id, created_at, id);
