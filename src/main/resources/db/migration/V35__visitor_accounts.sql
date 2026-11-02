-- 访客角色与学校邮箱绑定：非校园邮箱注册为只读访客，绑定校园邮箱后升级为普通用户。

ALTER TABLE foodtime.users DROP CONSTRAINT ck_users_role;
ALTER TABLE foodtime.users ADD CONSTRAINT ck_users_role
    CHECK (role IN ('visitor', 'user', 'admin', 'superadmin'));
COMMENT ON COLUMN foodtime.users.role IS 'visitor（只读访客）/ user / admin / superadmin';

ALTER TABLE foodtime.users ADD COLUMN school_email VARCHAR(254);
ALTER TABLE foodtime.users ADD COLUMN school_email_verified_at TIMESTAMPTZ;

ALTER TABLE foodtime.users ADD CONSTRAINT ck_users_school_email_normalized CHECK (
    school_email IS NULL OR (
        school_email = lower(btrim(school_email)) AND school_email LIKE '%@bjtu.edu.cn'
    )
);
ALTER TABLE foodtime.users ADD CONSTRAINT ck_users_school_email_verified
    CHECK ((school_email IS NULL) = (school_email_verified_at IS NULL));

CREATE UNIQUE INDEX ux_users_school_email
    ON foodtime.users (lower(school_email)) WHERE school_email IS NOT NULL;

-- 既有校园邮箱账号直接视为已绑定学校邮箱。
UPDATE foodtime.users
SET school_email = lower(btrim(email)),
    school_email_verified_at = COALESCE(email_verified_at, now())
WHERE lower(btrim(email)) LIKE '%@bjtu.edu.cn' AND school_email IS NULL;
