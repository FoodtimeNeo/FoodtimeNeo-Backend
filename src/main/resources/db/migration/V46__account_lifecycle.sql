ALTER TABLE foodtime.users DROP CONSTRAINT ck_users_status;
ALTER TABLE foodtime.users ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE foodtime.users ADD CONSTRAINT ck_users_status CHECK (status IN ('active','disabled','deleted'));
ALTER TABLE foodtime.users ADD CONSTRAINT ck_users_deleted_identity CHECK (
    (status='deleted')=(deleted_at IS NOT NULL) AND
    (status<>'deleted' OR (display_name='已注销用户' AND avatar_url IS NULL AND school_email IS NULL AND role='visitor')));

CREATE FUNCTION foodtime.protect_deleted_user() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status='deleted' AND
       (NEW.status,NEW.role,NEW.email,NEW.school_email,NEW.password_hash,NEW.display_name,NEW.avatar_url,NEW.deleted_at)
       IS DISTINCT FROM
       (OLD.status,OLD.role,OLD.email,OLD.school_email,OLD.password_hash,OLD.display_name,OLD.avatar_url,OLD.deleted_at)
    THEN RAISE EXCEPTION 'Deleted identity cannot be restored' USING ERRCODE='23514'; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER protect_deleted_user BEFORE UPDATE ON foodtime.users
    FOR EACH ROW EXECUTE FUNCTION foodtime.protect_deleted_user();

CREATE TABLE foodtime.email_cooldowns (
    email_hash CHAR(64) PRIMARY KEY,
    available_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_email_cooldowns_expiry ON foodtime.email_cooldowns(available_at);

CREATE TABLE foodtime.account_email_actions (
    user_id UUID NOT NULL REFERENCES foodtime.users(id) ON DELETE CASCADE,
    purpose VARCHAR(10) NOT NULL CHECK (purpose IN ('delete','unbind')),
    email VARCHAR(254) NOT NULL,
    password_version TIMESTAMPTZ NOT NULL,
    nonce UUID NOT NULL,
    code_hash CHAR(64) NOT NULL,
    state VARCHAR(10) NOT NULL CHECK (state IN ('reserved','sent','used')),
    send_day DATE NOT NULL,
    reserved_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 5),
    PRIMARY KEY(user_id,purpose)
);
