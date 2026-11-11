CREATE TABLE foodtime.user_feedback (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES foodtime.users(id),
    client_request_id UUID NOT NULL,
    device_key TEXT NOT NULL,
    submitted_as_visitor BOOLEAN NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('function', 'content', 'usage', 'suggestion', 'other')),
    content TEXT NOT NULL CHECK (char_length(content) BETWEEN 1 AND 5000),
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'processing', 'completed')),
    exported_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, client_request_id),
    CHECK ((status='pending' AND exported_at IS NULL)
        OR (status='processing' AND exported_at IS NOT NULL) OR status='completed')
);

CREATE INDEX ix_user_feedback_owner ON foodtime.user_feedback(user_id, created_at DESC, id);
CREATE INDEX ix_user_feedback_queue ON foodtime.user_feedback(status, created_at, id);
CREATE INDEX ix_user_feedback_device ON foodtime.user_feedback(device_key, created_at);

CREATE TABLE foodtime.feedback_replies (
    id UUID PRIMARY KEY,
    feedback_id UUID NOT NULL REFERENCES foodtime.user_feedback(id) ON DELETE CASCADE,
    replied_by UUID NOT NULL REFERENCES foodtime.users(id),
    content TEXT NOT NULL CHECK (char_length(content) BETWEEN 1 AND 5000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_feedback_replies_history ON foodtime.feedback_replies(feedback_id, created_at, id);

-- A retried CSV/manual action must not append the same reply a second time.
CREATE TABLE foodtime.feedback_actions (
    feedback_id UUID NOT NULL REFERENCES foodtime.user_feedback(id) ON DELETE CASCADE,
    action_key TEXT NOT NULL,
    PRIMARY KEY (feedback_id, action_key)
);
