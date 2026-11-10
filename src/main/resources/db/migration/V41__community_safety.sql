CREATE TABLE foodtime.user_blocks (
    blocker_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);

CREATE TABLE foodtime.content_reports (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    target_user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    target_type TEXT NOT NULL CHECK (target_type IN ('post', 'comment', 'message')),
    target_id UUID NOT NULL,
    post_id UUID,
    reason TEXT NOT NULL CHECK (char_length(reason) BETWEEN 1 AND 500),
    excerpt TEXT NOT NULL CHECK (char_length(excerpt) <= 2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES foodtime.users(id) ON DELETE SET NULL,
    UNIQUE (reporter_id, target_type, target_id),
    CHECK (reporter_id <> target_user_id)
);

CREATE INDEX ix_content_reports_pending ON foodtime.content_reports(created_at, id)
    WHERE reviewed_at IS NULL;
CREATE INDEX ix_content_reports_reporter_recent ON foodtime.content_reports(reporter_id, created_at DESC);
