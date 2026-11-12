-- 举报决定与双方回执：决定、公开理由、实际入箱时间和 7 天申诉截止。
ALTER TABLE foodtime.content_reports ADD COLUMN revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE foodtime.content_reports
    ADD CONSTRAINT ck_content_reports_revision CHECK (revision >= 1);

CREATE TABLE foodtime.report_decisions (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL UNIQUE REFERENCES foodtime.content_reports(id) ON DELETE CASCADE,
    outcome VARCHAR(16) NOT NULL,
    public_reason VARCHAR(500) NOT NULL,
    internal_note TEXT,
    reviewer_id UUID REFERENCES foodtime.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_report_decisions_outcome CHECK (outcome IN ('upheld', 'not_upheld')),
    CONSTRAINT ck_report_decisions_reason CHECK (char_length(btrim(public_reason)) BETWEEN 1 AND 500),
    CONSTRAINT ck_report_decisions_note CHECK (internal_note IS NULL OR char_length(internal_note) <= 2000)
);
CREATE INDEX ix_report_decisions_created ON foodtime.report_decisions (created_at DESC, id);

CREATE TABLE foodtime.report_decision_recipients (
    decision_id UUID NOT NULL REFERENCES foodtime.report_decisions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    side VARCHAR(16) NOT NULL,
    delivered_at TIMESTAMPTZ NOT NULL,
    appeal_deadline TIMESTAMPTZ NOT NULL,
    notification_event_id UUID REFERENCES foodtime.notification_events(id) ON DELETE SET NULL,
    PRIMARY KEY (decision_id, user_id),
    CONSTRAINT ck_report_recipients_side CHECK (side IN ('reporter', 'target')),
    CONSTRAINT ck_report_recipients_window CHECK (appeal_deadline >= delivered_at)
);
CREATE INDEX ix_report_recipients_user
    ON foodtime.report_decision_recipients (user_id, delivered_at DESC, decision_id);
