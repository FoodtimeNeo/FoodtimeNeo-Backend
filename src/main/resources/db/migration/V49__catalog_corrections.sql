-- 目录纠错提案：不改所属关系与目录图片，只允许名称、介绍、菜品价格。
-- revision 同时服务于管理员直接编辑与纠错批准的条件更新，避免旧基线覆盖新资料。
ALTER TABLE foodtime.dining_halls ADD COLUMN revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE foodtime.stalls ADD COLUMN revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE foodtime.dishes ADD COLUMN revision BIGINT NOT NULL DEFAULT 1;

CREATE TABLE foodtime.catalog_corrections (
    id UUID PRIMARY KEY,
    submitter_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    target_type VARCHAR(16) NOT NULL,
    target_id UUID NOT NULL,
    target_summary VARCHAR(200) NOT NULL,
    base_revision BIGINT NOT NULL,
    original_name VARCHAR(120) NOT NULL,
    original_description TEXT,
    original_price NUMERIC(10,2),
    proposed_name VARCHAR(120),
    proposed_description TEXT,
    proposed_price NUMERIC(10,2),
    changed_fields TEXT[] NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    review_note VARCHAR(500),
    reviewed_by UUID REFERENCES foodtime.users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    revision BIGINT NOT NULL DEFAULT 1,
    idempotency_key VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_catalog_corrections_target
        CHECK (target_type IN ('dining_hall', 'stall', 'dish')),
    CONSTRAINT ck_catalog_corrections_status
        CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT ck_catalog_corrections_fields
        CHECK (cardinality(changed_fields) BETWEEN 1 AND 3
               AND changed_fields <@ ARRAY['name', 'description', 'price']::text[]),
    CONSTRAINT ck_catalog_corrections_name
        CHECK (proposed_name IS NULL OR length(btrim(proposed_name)) BETWEEN 1 AND 120),
    CONSTRAINT ck_catalog_corrections_description
        CHECK (proposed_description IS NULL OR char_length(proposed_description) <= 500),
    CONSTRAINT ck_catalog_corrections_price
        CHECK (proposed_price IS NULL OR proposed_price >= 0),
    CONSTRAINT ck_catalog_corrections_decision
        CHECK ((status = 'pending' AND reviewed_by IS NULL AND reviewed_at IS NULL)
               OR (status <> 'pending' AND reviewed_at IS NOT NULL)),
    CONSTRAINT uq_catalog_corrections_idempotency
        UNIQUE (submitter_id, idempotency_key)
);
CREATE INDEX ix_catalog_corrections_submitter_created
    ON foodtime.catalog_corrections (submitter_id, created_at DESC, id);
CREATE INDEX ix_catalog_corrections_status_created
    ON foodtime.catalog_corrections (status, created_at DESC, id);
CREATE INDEX ix_catalog_corrections_target
    ON foodtime.catalog_corrections (target_type, target_id);

CREATE TABLE foodtime.catalog_correction_evidence (
    correction_id UUID NOT NULL REFERENCES foodtime.catalog_corrections(id) ON DELETE CASCADE,
    sort_order SMALLINT NOT NULL,
    image_url TEXT NOT NULL,
    uploader_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    purpose VARCHAR(24) NOT NULL DEFAULT 'correction_evidence',
    visibility VARCHAR(24) NOT NULL DEFAULT 'owner_and_reviewer',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (correction_id, sort_order),
    CONSTRAINT ck_catalog_correction_evidence_order CHECK (sort_order BETWEEN 0 AND 2),
    CONSTRAINT uq_catalog_correction_evidence_url UNIQUE (correction_id, image_url),
    CONSTRAINT ck_catalog_correction_evidence_visibility
        CHECK (visibility = 'owner_and_reviewer')
);
CREATE INDEX ix_catalog_correction_evidence_url
    ON foodtime.catalog_correction_evidence (image_url);

CREATE TABLE foodtime.catalog_correction_events (
    id UUID PRIMARY KEY,
    correction_id UUID NOT NULL REFERENCES foodtime.catalog_corrections(id) ON DELETE CASCADE,
    actor_id UUID REFERENCES foodtime.users(id) ON DELETE SET NULL,
    action VARCHAR(16) NOT NULL,
    public_note VARCHAR(500),
    internal_note TEXT,
    before_values JSONB NOT NULL DEFAULT '{}'::jsonb,
    after_values JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_catalog_correction_events_action
        CHECK (action IN ('submitted', 'approved', 'rejected'))
);
CREATE INDEX ix_catalog_correction_events_case
    ON foodtime.catalog_correction_events (correction_id, created_at, id);

CREATE TRIGGER trg_catalog_corrections_updated_at
    BEFORE UPDATE ON foodtime.catalog_corrections
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
