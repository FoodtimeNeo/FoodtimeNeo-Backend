-- Invitation codes for optional registration attribution. Only HMAC digests are stored;
-- plaintext codes are returned once at creation and never persisted.
-- Use schema-qualified names so this migration is independent of search_path.

CREATE TABLE foodtime.invitation_codes (
    id UUID PRIMARY KEY,
    code_hash TEXT NOT NULL,
    label VARCHAR(100),
    max_uses INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_by_user_id UUID REFERENCES foodtime.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invitation_codes_hash UNIQUE (code_hash),
    CONSTRAINT ck_invitation_codes_status CHECK (status IN ('active', 'disabled')),
    CONSTRAINT ck_invitation_codes_max_uses CHECK (max_uses IS NULL OR max_uses >= 1),
    CONSTRAINT ck_invitation_codes_used_count CHECK (used_count >= 0)
);
CREATE TRIGGER trg_invitation_codes_updated_at BEFORE UPDATE ON foodtime.invitation_codes
    FOR EACH ROW EXECUTE FUNCTION foodtime.set_updated_at();
COMMENT ON TABLE foodtime.invitation_codes IS '邀请码表：只存HMAC摘要，明文仅在创建时返回一次';
COMMENT ON COLUMN foodtime.invitation_codes.code_hash IS '邀请码的HMAC-SHA256摘要，不存明文';
COMMENT ON COLUMN foodtime.invitation_codes.max_uses IS '可复用次数上限，空表示不限次数';

ALTER TABLE foodtime.users
    ADD COLUMN invited_by_code_id UUID REFERENCES foodtime.invitation_codes(id) ON DELETE RESTRICT;
COMMENT ON COLUMN foodtime.users.invited_by_code_id IS '注册时使用的邀请码，可空；仅记录归因关系';
