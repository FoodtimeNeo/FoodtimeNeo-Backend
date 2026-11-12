-- 举报追问与双方申诉：公开回复、内部备注、证据关联、7 天期限与一次申诉约束。
CREATE TABLE foodtime.report_followups (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES foodtime.content_reports(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    author_role VARCHAR(8) NOT NULL,
    content VARCHAR(500) NOT NULL,
    internal_note VARCHAR(2000),
    client_request_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_report_followups_role CHECK (author_role IN ('reporter', 'staff')),
    CONSTRAINT ck_report_followups_content CHECK (char_length(btrim(content)) BETWEEN 1 AND 500),
    CONSTRAINT ck_report_followups_note CHECK (internal_note IS NULL OR char_length(internal_note) <= 2000),
    CONSTRAINT uq_report_followups_request UNIQUE (author_id, client_request_id)
);
CREATE INDEX ix_report_followups_report ON foodtime.report_followups (report_id, created_at, id);

CREATE TABLE foodtime.report_appeals (
    id UUID PRIMARY KEY,
    decision_id UUID NOT NULL REFERENCES foodtime.report_decisions(id) ON DELETE CASCADE,
    appellant_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    side VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'submitted',
    outcome VARCHAR(16),
    public_reason VARCHAR(500),
    internal_note TEXT,
    reviewer_id UUID REFERENCES foodtime.users(id) ON DELETE SET NULL,
    revision BIGINT NOT NULL DEFAULT 1,
    client_request_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    CONSTRAINT ck_report_appeals_side CHECK (side IN ('reporter', 'target')),
    CONSTRAINT ck_report_appeals_content CHECK (char_length(btrim(content)) BETWEEN 1 AND 2000),
    CONSTRAINT ck_report_appeals_status CHECK (status IN ('submitted', 'under_review', 'resolved')),
    CONSTRAINT ck_report_appeals_outcome CHECK (outcome IS NULL OR outcome IN ('upheld', 'rejected')),
    CONSTRAINT ck_report_appeals_reason CHECK (public_reason IS NULL OR char_length(btrim(public_reason)) BETWEEN 1 AND 500),
    CONSTRAINT ck_report_appeals_resolved CHECK (
        (status = 'resolved' AND outcome IS NOT NULL AND public_reason IS NOT NULL AND resolved_at IS NOT NULL)
        OR (status <> 'resolved' AND outcome IS NULL AND resolved_at IS NULL)
    ),
    CONSTRAINT ck_report_appeals_revision CHECK (revision >= 1),
    CONSTRAINT uq_report_appeals_once UNIQUE (decision_id, appellant_id, side),
    CONSTRAINT uq_report_appeals_request UNIQUE (appellant_id, client_request_id)
);
CREATE INDEX ix_report_appeals_queue ON foodtime.report_appeals (status, created_at, id);
CREATE INDEX ix_report_appeals_appellant ON foodtime.report_appeals (appellant_id, created_at DESC);

CREATE TABLE foodtime.report_evidence (
    id UUID PRIMARY KEY,
    kind VARCHAR(16) NOT NULL,
    ref_id UUID NOT NULL,
    uploader_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    sort_order SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_report_evidence_kind CHECK (kind IN ('followup', 'appeal')),
    CONSTRAINT ck_report_evidence_order CHECK (sort_order BETWEEN 0 AND 2),
    CONSTRAINT uq_report_evidence_url UNIQUE (kind, ref_id, image_url)
);
CREATE INDEX ix_report_evidence_ref ON foodtime.report_evidence (kind, ref_id, sort_order);
CREATE INDEX ix_report_evidence_url ON foodtime.report_evidence (image_url);
